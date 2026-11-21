import comum.*;
import servidor.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.net.ServerSocket;
import java.util.concurrent.*;

public class TesteJogo {
    interface Acao { void executar() throws Exception; }
    static void verificar(boolean ok, String mensagem) {
        if (!ok) throw new AssertionError(mensagem);
    }
    static void rejeita(Acao acao) throws Exception {
        try { acao.executar(); throw new AssertionError("Jogada deveria ser recusada"); }
        catch (JogadaInvalidaException esperado) { }
    }
    static void regras() throws Exception {
        Partida p = new Partida(2, 4);
        rejeita(() -> p.selecionar(0, 0, 0));
        p.adicionarJogador("Ana");
        p.adicionarJogador("Bruno");
        String[] numeros = p.matrizComoTexto().trim().split("\\s+");
        int[] valores = new int[numeros.length];
        for (int i = 0; i < valores.length; i++) valores[i] = Integer.parseInt(numeros[i]);
        for (int[] linha : p.estado().cartas)
            for (int valor : linha) verificar(valor == -1, "Vazamento de carta oculta");
        rejeita(() -> p.selecionar(-1, 0, 0));
        rejeita(() -> p.selecionar(1, 0, 0));
        rejeita(() -> p.selecionar(0, -1, 0));
        p.selecionar(0, 0, 0);
        rejeita(() -> p.selecionar(0, 0, 0));
        int diferente = 1;
        while (valores[diferente] == valores[0]) diferente++;
        p.selecionar(0, diferente / 4, diferente % 4);
        EstadoJogo erro = p.estado();
        verificar(erro.erros[0] == 1 && erro.aguardandoOcultar, "Erro não registrado");
        rejeita(() -> p.selecionar(0, 1, 3));
        verificar(!p.ocultarErro(erro.versao - 1), "Token antigo aceito");
        verificar(p.ocultarErro(erro.versao), "Ocultação não ocorreu");
        verificar(p.estado().vez == 1 && p.estado().cartas[0][0] == -1, "Troca de turno");
        // Cada jogador encontra dois pares: acerto mantém turno e empate é apurado.
        for (int valor = 0; valor < 4; valor++) {
            if (valor == 2) {
                int a = -1, b = -1;
                for (int i = 0; i < 8; i++) {
                    if (valores[i] == 2) a = i;
                    if (valores[i] == 3) b = i;
                }
                p.selecionar(1, a / 4, a % 4);
                p.selecionar(1, b / 4, b % 4);
                p.ocultarErro(p.estado().versao);
            }
            int jogador = valor < 2 ? 1 : 0;
            for (int i = 0; i < 8; i++)
                if (valores[i] == valor) p.selecionar(jogador, i / 4, i % 4);
            verificar(p.estado().vez == jogador, "Acerto trocou o turno");
        }
        verificar(p.estado().encerrada && !p.estado().ativa && p.estado().mensagem.contains("empate"), "Empate");
        rejeita(() -> p.selecionar(0, 0, 0));
        rejeita(() -> p.definirRevanche(-1, true));
        p.definirRevanche(0, true);
        verificar(p.estado().encerrada && p.estado().revanche[0] && !p.estado().revanche[1], "Uma confirmação não reinicia");
        long confirmada = p.estado().versao;
        p.definirRevanche(0, true);
        verificar(p.estado().versao == confirmada, "Pedido repetido deve ser idempotente");
        p.definirRevanche(0, false);
        verificar(!p.estado().revanche[0], "Cancelar confirmação");
        p.definirRevanche(1, true);
        p.definirRevanche(0, true);
        verificar(p.estado().ativa && !p.estado().encerrada && p.estado().vez == 0
                && p.estado().acertos[0] == 0 && p.estado().acertos[1] == 0
                && p.estado().erros[0] == 0 && p.estado().erros[1] == 0
                && !p.estado().revanche[0] && !p.estado().revanche[1], "Revanche limpa o estado");
        for (int[] linha : p.estado().cartas)
            for (int valor : linha) verificar(valor == -1, "Revanche oculta cartas");
        rejeita(() -> p.definirRevanche(0, true));
        p.removerJogador(0);
        p.adicionarJogador("Carla");
        verificar(p.estado().ativa && p.estado().acertos[0] == 0 && p.estado().acertos[1] == 0, "Reinício");
        // Cancelar um erro pendente impede o temporizador de alterar a nova partida.
        numeros = p.matrizComoTexto().trim().split("\\s+");
        int outro = 1;
        while (numeros[outro].equals(numeros[0])) outro++;
        p.selecionar(0, 0, 0);
        p.selecionar(0, outro / 4, outro % 4);
        long antigo = p.estado().versao;
        p.reiniciar();
        verificar(!p.ocultarErro(antigo) && p.estado().ativa && !p.estado().aguardandoOcultar
                && p.estado().erros[0] == 0 && p.estado().vez == 0, "Hard-reset durante erro");
        verificar(p.estado().cartas[0][0] == -1, "Hard-reset oculta cartas");
        p.removerJogador(1);
        p.reiniciar();
        verificar(!p.estado().ativa && p.nome(0).equals("Carla") && p.nome(1) == null, "Reset sem adversário");
        p.adicionarJogador("Davi");
        verificar(!p.ocultarErro(antigo) && p.estado().vez == 0, "Temporizador de partida anterior");
        // Alterar uma cópia pública não altera o estado oficial.
        EstadoJogo copia = p.estado();
        copia.cartas[0][0] = 99;
        copia.acertos[0] = 99;
        verificar(p.estado().cartas[0][0] == -1 && p.estado().acertos[0] == 0, "Cópia isolada");
        System.out.println("OK: regras, turnos, erro, pares, empate, reinício e estado público");
    }

    static class Cliente extends UnicastRemoteObject implements IClienteCallback {
        final BlockingQueue<EstadoJogo> eventos = new LinkedBlockingQueue<>();
        Cliente() throws Exception { super(0); }
        public void estadoAtualizado(EstadoJogo estado) { eventos.add(estado); }
        EstadoJogo aguardar(long versao) throws Exception {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (true) {
                long restante = limite - System.nanoTime();
                EstadoJogo e = eventos.poll(Math.max(0, restante), TimeUnit.NANOSECONDS);
                if (e == null) throw new AssertionError("Callback não recebido");
                if (e.versao >= versao) return e;
            }
        }
    }
    static void rmi() throws Exception {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) { porta = socket.getLocalPort(); }
        Registry registry = LocateRegistry.createRegistry(porta);
        ServidorJogoImpl impl = new ServidorJogoImpl(0, 1, 2);
        Cliente a = new Cliente(), b = new Cliente();
        try {
            registry.rebind(IServidorJogo.NOME_REGISTRO, impl);
            IServidorJogo remoto = (IServidorJogo) LocateRegistry.getRegistry("127.0.0.1", porta)
                    .lookup(IServidorJogo.NOME_REGISTRO);
            int ana = remoto.entrar("Ana", a), bruno = remoto.entrar("Bruno", b);
            rejeita(() -> remoto.entrar("Terceiro", a));
            rejeita(() -> remoto.revelarCarta(bruno, 0, 0));
            remoto.revelarCarta(ana, 0, 0);
            long v = remoto.obterEstado().versao;
            verificar(a.aguardar(v).cartas[0][0] == 0 && b.aguardar(v).cartas[0][0] == 0,
                    "Revelação nos dois clientes");
            remoto.revelarCarta(ana, 0, 1);
            v = remoto.obterEstado().versao;
            verificar(a.aguardar(v).mensagem.contains("Ana venceu") && b.aguardar(v).encerrada,
                    "Resultado nos dois clientes");
            rejeita(() -> remoto.definirRevanche(-1, true));
            remoto.definirRevanche(ana, true);
            v = remoto.obterEstado().versao;
            verificar(a.aguardar(v).revanche[ana] && b.aguardar(v).encerrada, "Confirmação distribuída");
            remoto.definirRevanche(bruno, true);
            v = remoto.obterEstado().versao;
            verificar(a.aguardar(v).ativa && b.aguardar(v).acertos[ana] == 0, "Revanche por RMI");
            remoto.revelarCarta(ana, 0, 0);
            impl.reiniciar();
            v = remoto.obterEstado().versao;
            verificar(a.aguardar(v).cartas[0][0] == -1 && b.aguardar(v).cartas[0][0] == -1,
                    "Hard-reset distribuído");
            remoto.sair(bruno);
            verificar(!remoto.obterEstado().ativa, "Saída");
            remoto.entrar("Carla", b);
            verificar(remoto.obterEstado().ativa && remoto.obterEstado().acertos[0] == 0, "Nova sala");
            v = remoto.obterEstado().versao;
            a.aguardar(v);
            b.aguardar(v);
        } finally {
            UnicastRemoteObject.unexportObject(a, true);
            UnicastRemoteObject.unexportObject(b, true);
            UnicastRemoteObject.unexportObject(impl, true);
            UnicastRemoteObject.unexportObject(registry, true);
        }
        System.out.println("OK: registry, lookup, exceções e callbacks RMI reais em loopback");
    }
    static void terminal() throws Exception {
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) { porta = socket.getLocalPort(); }
        String executavelJava = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process processo = new ProcessBuilder(executavelJava, "-cp", System.getProperty("java.class.path"),
                "servidor.ServidorMain", "127.0.0.1", String.valueOf(porta), "1", "2")
                .redirectErrorStream(true).start();
        BlockingQueue<String> linhas = new LinkedBlockingQueue<>();
        Thread leitura = new Thread(() -> {
            try (java.io.BufferedReader leitor = new java.io.BufferedReader(
                    new java.io.InputStreamReader(processo.getInputStream()))) {
                String linha;
                while ((linha = leitor.readLine()) != null) linhas.add(linha);
            } catch (java.io.IOException ignorada) { }
        });
        leitura.setDaemon(true);
        leitura.start();
        try {
            aguardarLinha(linhas, "Aguardando jogadores");
            IServidorJogo remoto = (IServidorJogo) LocateRegistry.getRegistry("127.0.0.1", porta)
                    .lookup(IServidorJogo.NOME_REGISTRO);
            long antes = remoto.obterEstado().versao;
            processo.getOutputStream().write("r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            processo.getOutputStream().flush();
            aguardarLinha(linhas, "HARD-RESET");
            verificar(remoto.obterEstado().versao > antes && !remoto.obterEstado().ativa,
                    "Comando do terminal não reiniciou o estado");
        } finally {
            processo.destroy();
            if (!processo.waitFor(3, TimeUnit.SECONDS)) processo.destroyForcibly();
        }
        System.out.println("OK: comando r + Enter no processo real do servidor");
    }
    static void aguardarLinha(BlockingQueue<String> linhas, String trecho) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (true) {
            String linha = linhas.poll(Math.max(0, limite - System.nanoTime()), TimeUnit.NANOSECONDS);
            if (linha == null) throw new AssertionError("Servidor não emitiu: " + trecho);
            if (linha.contains(trecho)) return;
        }
    }
    public static void main(String[] args) {
        try { regras(); rmi(); terminal(); }
        catch (Throwable erro) { erro.printStackTrace(); System.exit(1); }
    }
}

package servidor;

import comum.IClienteCallback;
import comum.IServidorJogo;
import comum.JogadaInvalidaException;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Implementação do objeto remoto (middleware)

public class ServidorJogoImpl extends UnicastRemoteObject implements IServidorJogo {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Partida partida;
    private final IClienteCallback[] clientes = new IClienteCallback[2];

    // Envia os callbacks numa thread separada, na ordem em que os eventos aconteceram,
    // para um cliente lento não travar o servidor
    private final ExecutorService notificador = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "notificador-callbacks");
        t.setDaemon(true);
        return t;
    });

    // Ação que o servidor executa em cada cliente (um callback RMI)
    private interface Callback {
        void enviar(IClienteCallback cliente) throws RemoteException;
    }

    public ServidorJogoImpl(int porta, int linhas, int colunas) throws RemoteException {
        super(porta);
        this.partida = new Partida(linhas, colunas);
    }

    @Override
    public synchronized int entrar(String nome, IClienteCallback callback) throws JogadaInvalidaException {
        log("RMI <- entrar(nome=\"" + nome + "\") de " + hostDoCliente());

        if (nome == null || nome.isBlank()) {
            throw new JogadaInvalidaException("Informe um nome.");
        }
        int id = partida.adicionarJogador(nome.trim());
        if (id < 0) {
            log("   partida cheia, entrada recusada");
            throw new JogadaInvalidaException("A partida já tem dois jogadores.");
        }
        clientes[id] = callback;
        log("   " + partida.nome(id) + " é o jogador " + (id + 1));

        if (partida.temDoisJogadores()) {
            partida.sortear();
            log("   PARTIDA INICIADA. Matriz sorteada:\n" + partida.matrizComoTexto());
            String msg = "Partida iniciada: " + partida.nome(0) + " x " + partida.nome(1);
            difundir("notificar", c -> c.notificar(msg));
        } else {
            difundir("notificar", c -> c.notificar("Aguardando adversário..."));
        }
        return id;
    }

    @Override
    public synchronized int[] obterTamanho() {
        return new int[]{partida.getLinhas(), partida.getColunas()};
    }

    @Override
    public synchronized void revelarCarta(int jogadorId, int linha, int coluna) throws JogadaInvalidaException {
        log(String.format("RMI <- revelarCarta(jogador=%d, linha=%d, coluna=%d) de %s",
                jogadorId, linha, coluna, hostDoCliente()));
        int valor = partida.valor(linha, coluna);
        String jogador = partida.nome(jogadorId);
        // Atualização em tempo real: os DOIS clientes recebem a carta revelada
        difundir("cartaRevelada", c -> c.cartaRevelada(linha, coluna, valor, jogador));
        // TODO: validar a vez do jogador
    }

    @Override
    public synchronized void ocultarCarta(int jogadorId, int linha, int coluna) throws JogadaInvalidaException {
        log(String.format("RMI <- ocultarCarta(jogador=%d, linha=%d, coluna=%d) de %s",
                jogadorId, linha, coluna, hostDoCliente()));
        partida.valor(linha, coluna); // só valida a posição
        String jogador = partida.nome(jogadorId);
        difundir("cartaOcultada", c -> c.cartaOcultada(linha, coluna, jogador));
    }

    @Override
    public synchronized void sair(int jogadorId) {
        log("RMI <- sair(jogador=" + jogadorId + ") de " + hostDoCliente());
        if (jogadorId < 0 || jogadorId > 1) return;
        String nome = partida.nome(jogadorId);
        clientes[jogadorId] = null;
        partida.removerJogador(jogadorId);
        difundir("notificar", c -> c.notificar(nome + " saiu da partida."));
    }

    // Executa o mesmo CALLBACK RMI em todos os clientes conectados
    private void difundir(String metodo, Callback callback) {
        IClienteCallback[] alvos = clientes.clone();
        String[] nomes = {partida.nome(0), partida.nome(1)};
        notificador.submit(() -> {
            for (int id = 0; id < 2; id++) {
                if (alvos[id] == null) continue;
                try {
                    callback.enviar(alvos[id]);
                    log("RMI -> " + metodo + "() em " + nomes[id]);
                } catch (RemoteException e) {
                    log("   falha no callback de " + nomes[id] + ": " + e.getMessage());
                }
            }
        });
    }

    private static String hostDoCliente() {
        try {
            return getClientHost(); // IP de quem fez a chamada RMI
        } catch (ServerNotActiveException e) {
            return "local";
        }
    }

    static void log(String msg) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + msg);
    }
}

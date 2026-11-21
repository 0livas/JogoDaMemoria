package servidor;

import comum.EstadoJogo;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServidorJogoImpl extends UnicastRemoteObject implements IServidorJogo {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Partida partida;
    private final IClienteCallback[] clientes = new IClienteCallback[2];
    private final ExecutorService notificador = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "notificador-callbacks");
        t.setDaemon(true);
        return t;
    });
    private final ScheduledExecutorService temporizador = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ocultar-par");
        t.setDaemon(true);
        return t;
    });

    public ServidorJogoImpl(int porta, int linhas, int colunas) throws RemoteException {
        super(porta);
        partida = new Partida(linhas, colunas);
    }

    @Override
    public synchronized int entrar(String nome, IClienteCallback callback) throws JogadaInvalidaException {
        log("RMI <- entrar(" + nome + ") de " + hostDoCliente());
        if (nome == null || nome.isBlank()) throw new JogadaInvalidaException("Informe um nome.");
        if (callback == null) throw new JogadaInvalidaException("Callback obrigatório.");
        int id = partida.adicionarJogador(nome.trim());
        if (id < 0) throw new JogadaInvalidaException("A partida já tem dois jogadores.");
        clientes[id] = callback;
        if (partida.temDoisJogadores()) log("PARTIDA INICIADA. Matriz sorteada:\n" + partida.matrizComoTexto());
        difundir();
        return id;
    }

    @Override
    public synchronized EstadoJogo obterEstado() { return partida.estado(); }

    @Override
    public synchronized void revelarCarta(int jogadorId, int linha, int coluna) throws JogadaInvalidaException {
        log(String.format("RMI <- revelarCarta(jogador=%d, linha=%d, coluna=%d) de %s",
                jogadorId, linha, coluna, hostDoCliente()));
        partida.selecionar(jogadorId, linha, coluna);
        EstadoJogo estado = partida.estado();
        difundir();
        if (estado.aguardandoOcultar) {
            temporizador.schedule(() -> {
                synchronized (ServidorJogoImpl.this) {
                    if (partida.ocultarErro(estado.versao)) difundir();
                }
            }, 1500, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public synchronized void definirRevanche(int jogadorId, boolean aceita) throws JogadaInvalidaException {
        log("RMI <- definirRevanche(jogador=" + jogadorId + ", aceita=" + aceita + ") de " + hostDoCliente());
        partida.definirRevanche(jogadorId, aceita);
        if (partida.estado().ativa) log("REVANCHE. Matriz sorteada:\n" + partida.matrizComoTexto());
        difundir();
    }

    // Disponível apenas no objeto local; não faz parte da interface RMI.
    public synchronized void reiniciar() {
        partida.reiniciar();
        log("HARD-RESET. Matriz sorteada:\n" + partida.matrizComoTexto());
        difundir();
    }

    @Override
    public synchronized void sair(int jogadorId) {
        log("RMI <- sair(jogador=" + jogadorId + ") de " + hostDoCliente());
        if (jogadorId < 0 || jogadorId > 1) return;
        clientes[jogadorId] = null;
        partida.removerJogador(jogadorId);
        difundir();
    }

    // Captura uma cópia consistente e envia os eventos na ordem das jogadas.
    private void difundir() {
        EstadoJogo estado = partida.estado();
        IClienteCallback[] alvos = clientes.clone();
        notificador.submit(() -> {
            for (int id = 0; id < 2; id++) {
                if (alvos[id] == null) continue;
                try {
                    alvos[id].estadoAtualizado(estado);
                    log("RMI -> estadoAtualizado(v=" + estado.versao + ") em " + estado.nomes[id]);
                } catch (RemoteException e) {
                    log("Falha no callback de " + estado.nomes[id] + ": " + e.getMessage());
                    synchronized (ServidorJogoImpl.this) {
                        if (clientes[id] == alvos[id]) sair(id);
                    }
                }
            }
        });
    }

    private static String hostDoCliente() {
        try { return getClientHost(); }
        catch (ServerNotActiveException e) { return "local"; }
    }
    static void log(String msg) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + msg);
    }
}

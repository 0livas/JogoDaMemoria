package servidor;

import comum.IClienteCallback;
import comum.IServidorJogo;
import comum.JogadaInvalidaException;

import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

//Implementação do objeto remoto (middleware)

public class ServidorJogoImpl extends UnicastRemoteObject implements IServidorJogo {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Partida partida;
    private final IClienteCallback[] clientes = new IClienteCallback[2];

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
            notificarTodos("Partida iniciada: " + partida.nome(0) + " x " + partida.nome(1));
        } else {
            notificarTodos("Aguardando adversário...");
        }
        return id;
    }

    @Override
    public synchronized int[] obterTamanho() {
        return new int[]{partida.getLinhas(), partida.getColunas()};
    }

    @Override
    public synchronized int revelarCarta(int linha, int coluna) throws JogadaInvalidaException {
        log(String.format("RMI <- revelarCarta(linha=%d, coluna=%d) de %s", linha, coluna, hostDoCliente()));
        // TODO: validar a vez do jogador e avisar o outro cliente
        return partida.valor(linha, coluna);
    }

    @Override
    public synchronized void sair(int jogadorId) {
        log("RMI <- sair(jogador=" + jogadorId + ") de " + hostDoCliente());
        if (jogadorId < 0 || jogadorId > 1) return;
        clientes[jogadorId] = null;
        partida.removerJogador(jogadorId);
    }

    // Avisa os clientes via CALLBACK RMI
    private void notificarTodos(String mensagem) {
        for (int id = 0; id < 2; id++) {
            if (clientes[id] == null) continue;
            try {
                clientes[id].notificar(mensagem);
                log("RMI -> notificar() em " + partida.nome(id));
            } catch (RemoteException e) {
                log("   falha no callback de " + partida.nome(id) + ": " + e.getMessage());
            }
        }
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

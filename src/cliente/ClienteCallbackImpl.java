package cliente;

import comum.IClienteCallback;

import javax.swing.SwingUtilities;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * As chamadas chegam em threads do RMI; como o Swing só pode ser alterado na
 * thread de eventos (EDT), repassamos com SwingUtilities.invokeLater.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements IClienteCallback {
    private static final long serialVersionUID = 1L;

    private final transient JanelaJogo janela;

    public ClienteCallbackImpl(int porta, JanelaJogo janela) throws RemoteException {
        super(porta); // 0 = porta livre qualquer
        this.janela = janela;
    }

    @Override
    public void notificar(String mensagem) {
        SwingUtilities.invokeLater(() -> janela.mostrarMensagem(mensagem));
    }

    @Override
    public void cartaRevelada(int linha, int coluna, int valor, String jogador) {
        SwingUtilities.invokeLater(() -> janela.mostrarCarta(linha, coluna, valor, jogador));
    }

    @Override
    public void cartaOcultada(int linha, int coluna, String jogador) {
        SwingUtilities.invokeLater(() -> janela.esconderCarta(linha, coluna, jogador));
    }

    @Override
    public void ping() {
        // nada: se a chamada chegou, o cliente está vivo
    }
}

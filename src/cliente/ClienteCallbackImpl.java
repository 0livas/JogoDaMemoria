package cliente;

import comum.IClienteCallback;

import javax.swing.SwingUtilities;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

/**
 * As chamadas chegam em threads do RMI; como o Swing só pode ser alterado na
 * thread de eventos (EDT), repassamos com SwingUtilities.invokeLater.
 */
public class ClienteCallbackImpl extends UnicastRemoteObject implements IClienteCallback {
    private static final long serialVersionUID = 1L;

    private final transient Consumer<String> aoNotificar;

    public ClienteCallbackImpl(int porta, Consumer<String> aoNotificar) throws RemoteException {
        super(porta); // 0 = porta livre qualquer
        this.aoNotificar = aoNotificar;
    }

    @Override
    public void notificar(String mensagem) {
        SwingUtilities.invokeLater(() -> aoNotificar.accept(mensagem));
    }

    @Override
    public void ping() {
        // nada: se a chamada chegou, o cliente está vivo
    }
}

package cliente;

import comum.EstadoJogo;
import comum.IClienteCallback;
import javax.swing.SwingUtilities;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClienteCallbackImpl extends UnicastRemoteObject implements IClienteCallback {
    private static final long serialVersionUID = 1L;
    private final transient JanelaJogo janela;
    public ClienteCallbackImpl(int porta, JanelaJogo janela) throws RemoteException {
        super(porta);
        this.janela = janela;
    }
    @Override
    public void estadoAtualizado(EstadoJogo estado) {
        SwingUtilities.invokeLater(() -> janela.atualizarEstado(estado));
    }
}

package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClienteCallback extends Remote {
    void estadoAtualizado(EstadoJogo estado) throws RemoteException;
}

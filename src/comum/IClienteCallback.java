package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClienteCallback extends Remote {

    //O servidor manda avisos para o cliente (ex.: partida iniciada)
    void notificar(String mensagem) throws RemoteException;

    //Usado pelo servidor para verificar se o cliente ainda está conectado
    void ping() throws RemoteException;

    // TODO: atualizarEstado(...) para sincronizar o tabuleiro dos dois clientes em tempo real
}

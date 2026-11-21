package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClienteCallback extends Remote {

    //O servidor manda avisos para o cliente (ex.: partida iniciada)
    void notificar(String mensagem) throws RemoteException;

    //Atualização em tempo real: o servidor avisa TODOS os clientes que uma carta foi revelada
    void cartaRevelada(int linha, int coluna, int valor, String jogador) throws RemoteException;

    //Atualização em tempo real: o servidor avisa TODOS os clientes que uma carta foi escondida
    void cartaOcultada(int linha, int coluna, String jogador) throws RemoteException;

    //Usado pelo servidor para verificar se o cliente ainda está conectado
    void ping() throws RemoteException;
}

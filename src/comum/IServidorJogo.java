package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServidorJogo extends Remote {
    String NOME_REGISTRO = "JogoDaMemoria";
    int PORTA_PADRAO = 1099;

    int entrar(String nome, IClienteCallback callback) throws RemoteException, JogadaInvalidaException;
    EstadoJogo obterEstado() throws RemoteException;
    // O cliente apenas solicita uma seleção. Todas as decisões ficam no servidor.
    void revelarCarta(int jogadorId, int linha, int coluna) throws RemoteException, JogadaInvalidaException;
    void definirRevanche(int jogadorId, boolean aceita) throws RemoteException, JogadaInvalidaException;
    void sair(int jogadorId) throws RemoteException;
}

package comum;

import java.rmi.Remote;
import java.rmi.RemoteException;

//Interface REMOTA do middleware (servidor) do jogo da memória

public interface IServidorJogo extends Remote {

    String NOME_REGISTRO = "JogoDaMemoria";

    // Porta padrão do RMI Registry
    int PORTA_PADRAO = 1099;

    /**
     * Entra na partida
     * @param nome     nome do jogador
     * @param callback objeto remoto do PRÓPRIO cliente; o servidor usa para avisar o cliente
     * @return id do jogador (0 ou 1)
     * @throws JogadaInvalidaException se a partida já tiver dois jogadores
     */
    int entrar(String nome, IClienteCallback callback) throws RemoteException, JogadaInvalidaException;

    // Retorna {linhas, colunas} do tabuleiro
    int[] obterTamanho() throws RemoteException;

    // Retorna o valor (número da imagem) da carta na posição
    int revelarCarta(int linha, int coluna) throws RemoteException, JogadaInvalidaException;

    void sair(int jogadorId) throws RemoteException;

    // TODO: controle de turno (só quem tem a vez joga)
    // TODO: estado da partida (acertos, erros, ordem das jogadas) e fim de jogo
}

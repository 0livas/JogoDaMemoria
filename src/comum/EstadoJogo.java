package comum;

import java.io.Serializable;

/** Cópia do estado público: -1 representa uma carta oculta. */
public final class EstadoJogo implements Serializable {
    private static final long serialVersionUID = 1L;
    public final long versao;
    public final boolean[] revanche;
    public final int[][] cartas;
    public final String[] nomes;
    public final int[] acertos, erros;
    public final int vez;
    public final boolean ativa, aguardandoOcultar, encerrada;
    public final String mensagem;

    public EstadoJogo(long versao, int[][] cartas, String[] nomes, int[] acertos,
                      int[] erros, int vez, boolean ativa, boolean aguardandoOcultar,
                      boolean encerrada, String mensagem, boolean[] revanche) {
        this.versao = versao;
        this.revanche = revanche.clone();
        this.cartas = new int[cartas.length][];
        for (int l = 0; l < cartas.length; l++) this.cartas[l] = cartas[l].clone();
        this.nomes = nomes.clone();
        this.acertos = acertos.clone();
        this.erros = erros.clone();
        this.vez = vez;
        this.ativa = ativa;
        this.aguardandoOcultar = aguardandoOcultar;
        this.encerrada = encerrada;
        this.mensagem = mensagem;
    }
}

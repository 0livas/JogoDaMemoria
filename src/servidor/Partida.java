package servidor;

import comum.EstadoJogo;
import comum.JogadaInvalidaException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Regras e estado oficial. Acesso serializado pelo monitor do servidor. */
public class Partida {
    private final int linhas, colunas;
    private final String[] nomes = new String[2];
    private final int[] acertos = new int[2], erros = new int[2];
    private final boolean[] revanche = new boolean[2];
    private int[][] matriz;
    private boolean[][] visiveis;
    private int vez, primeira = -1, segunda = -1, encontrados;
    private boolean ativa, aguardandoOcultar, encerrada;
    private long versao;
    private String mensagem = "Aguardando adversário...";

    public Partida(int linhas, int colunas) {
        long total = (long) linhas * colunas;
        if (linhas <= 0 || colunas <= 0 || total % 2 != 0 || total > 36)
            throw new IllegalArgumentException("Use um tabuleiro com número par de cartas e até 18 pares.");
        this.linhas = linhas;
        this.colunas = colunas;
        visiveis = new boolean[linhas][colunas];
    }

    public int adicionarJogador(String nome) {
        for (int id = 0; id < 2; id++) {
            if (nomes[id] == null) {
                nomes[id] = nome;
                versao++;
                if (temDoisJogadores()) sortear();
                return id;
            }
        }
        return -1;
    }

    public void removerJogador(int id) {
        if (id < 0 || id > 1 || nomes[id] == null) return;
        String nome = nomes[id];
        nomes[id] = null;
        ativa = aguardandoOcultar = encerrada = false;
        matriz = null;
        visiveis = new boolean[linhas][colunas];
        Arrays.fill(acertos, 0);
        Arrays.fill(erros, 0);
        Arrays.fill(revanche, false);
        primeira = segunda = -1;
        encontrados = 0;
        mensagem = nome + " saiu. Aguardando adversário para uma nova partida.";
        versao++;
    }

    public boolean temDoisJogadores() { return nomes[0] != null && nomes[1] != null; }
    public String nome(int id) { return id >= 0 && id < 2 ? nomes[id] : null; }

    private void sortear() {
        List<Integer> valores = new ArrayList<>();
        for (int v = 0; v < linhas * colunas / 2; v++) {
            valores.add(v);
            valores.add(v);
        }
        Collections.shuffle(valores, new SecureRandom());
        matriz = new int[linhas][colunas];
        visiveis = new boolean[linhas][colunas];
        for (int i = 0; i < valores.size(); i++) matriz[i / colunas][i % colunas] = valores.get(i);
        Arrays.fill(acertos, 0);
        Arrays.fill(erros, 0);
        Arrays.fill(revanche, false);
        vez = encontrados = 0;
        primeira = segunda = -1;
        ativa = true;
        aguardandoOcultar = encerrada = false;
        mensagem = "Partida iniciada. Vez de " + nomes[vez] + ".";
    }

    public void definirRevanche(int jogador, boolean aceita) throws JogadaInvalidaException {
        if (nome(jogador) == null) throw new JogadaInvalidaException("Jogador inválido.");
        if (!encerrada || !temDoisJogadores())
            throw new JogadaInvalidaException("A revanche só pode ser solicitada após o fim da partida.");
        if (revanche[jogador] == aceita) return;
        revanche[jogador] = aceita;
        if (revanche[0] && revanche[1]) reiniciar();
        else versao++;
    }

    /** Reinício administrativo local: mantém os jogadores e invalida temporizadores. */
    public void reiniciar() {
        if (temDoisJogadores()) {
            sortear();
        } else {
            matriz = null;
            visiveis = new boolean[linhas][colunas];
            Arrays.fill(acertos, 0);
            Arrays.fill(erros, 0);
            Arrays.fill(revanche, false);
            vez = encontrados = 0;
            primeira = segunda = -1;
            ativa = aguardandoOcultar = encerrada = false;
            mensagem = "Partida reiniciada. Aguardando adversário...";
        }
        versao++;
    }

    public void selecionar(int jogador, int linha, int coluna) throws JogadaInvalidaException {
        if (nome(jogador) == null) throw new JogadaInvalidaException("Jogador inválido.");
        if (!ativa) throw new JogadaInvalidaException(encerrada ? "A partida terminou." : "Aguardando adversário.");
        if (aguardandoOcultar) throw new JogadaInvalidaException("Aguarde as cartas serem ocultadas.");
        if (jogador != vez) throw new JogadaInvalidaException("É a vez de " + nomes[vez] + ".");
        if (linha < 0 || linha >= linhas || coluna < 0 || coluna >= colunas)
            throw new JogadaInvalidaException("Posição inválida.");
        if (visiveis[linha][coluna]) throw new JogadaInvalidaException("Esta carta já está revelada.");
        visiveis[linha][coluna] = true;
        int posicao = linha * colunas + coluna;
        if (primeira < 0) {
            primeira = posicao;
            mensagem = nomes[vez] + ": selecione a segunda carta.";
        } else {
            segunda = posicao;
            if (matriz[primeira / colunas][primeira % colunas] == matriz[linha][coluna]) {
                acertos[vez]++;
                encontrados++;
                primeira = segunda = -1;
                mensagem = nomes[vez] + " acertou e continua jogando.";
                if (encontrados == linhas * colunas / 2) {
                    ativa = false;
                    encerrada = true;
                    mensagem = acertos[0] == acertos[1] ? "Fim de jogo: empate!"
                            : "Fim de jogo: " + nomes[acertos[0] > acertos[1] ? 0 : 1] + " venceu!";
                }
            } else {
                erros[vez]++;
                aguardandoOcultar = true;
                mensagem = nomes[vez] + " errou. Aguarde as cartas serem ocultadas.";
            }
        }
        versao++;
    }

    // O token impede um temporizador antigo de alterar uma nova partida.
    public boolean ocultarErro(long token) {
        if (!aguardandoOcultar || token != versao) return false;
        visiveis[primeira / colunas][primeira % colunas] = false;
        visiveis[segunda / colunas][segunda % colunas] = false;
        primeira = segunda = -1;
        aguardandoOcultar = false;
        vez = 1 - vez;
        mensagem = "Vez de " + nomes[vez] + ".";
        versao++;
        return true;
    }

    public EstadoJogo estado() {
        int[][] cartas = new int[linhas][colunas];
        for (int l = 0; l < linhas; l++)
            for (int c = 0; c < colunas; c++)
                cartas[l][c] = matriz != null && visiveis[l][c] ? matriz[l][c] : -1;
        return new EstadoJogo(versao, cartas, nomes, acertos, erros, vez,
                ativa, aguardandoOcultar, encerrada, mensagem, revanche);
    }

    public String matrizComoTexto() {
        if (matriz == null) return "(sem matriz)";
        StringBuilder sb = new StringBuilder();
        for (int[] linha : matriz) {
            for (int v : linha) sb.append(String.format("%3d", v));
            sb.append('\n');
        }
        return sb.toString();
    }
}

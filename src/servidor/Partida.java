package servidor;

import comum.JogadaInvalidaException;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Guarda a matriz de pares da partida (fica só no servidor)
public class Partida {

    private final int linhas;
    private final int colunas;
    private final Random random = new SecureRandom();
    private final String[] nomes = new String[2];

    // Matriz com os pares de números aleatórios (cada número = uma imagem)
    private int[][] matriz;

    public Partida(int linhas, int colunas) {
        if (linhas <= 0 || colunas <= 0 || (linhas * colunas) % 2 != 0) {
            throw new IllegalArgumentException("O tabuleiro precisa ter um número par de cartas.");
        }
        this.linhas = linhas;
        this.colunas = colunas;
    }

    // retorna o id do jogador (0 ou 1), ou -1 se a partida estiver cheia
    public int adicionarJogador(String nome) {
        for (int id = 0; id < 2; id++) {
            if (nomes[id] == null) {
                nomes[id] = nome;
                return id;
            }
        }
        return -1;
    }

    public void removerJogador(int id) {
        if (id >= 0 && id < 2) nomes[id] = null;
    }

    public boolean temDoisJogadores() {
        return nomes[0] != null && nomes[1] != null;
    }

    public String nome(int id) {
        return (id >= 0 && id < 2) ? nomes[id] : null;
    }

    // Sorteia a matriz de pares
    public void sortear() {
        int pares = linhas * colunas / 2;

        // Cada valor 0..pares-1 aparece exatamente duas vezes; depois embaralha
        List<Integer> valores = new ArrayList<>();
        for (int v = 0; v < pares; v++) {
            valores.add(v);
            valores.add(v);
        }
        Collections.shuffle(valores, random);

        matriz = new int[linhas][colunas];
        int i = 0;
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                matriz[l][c] = valores.get(i++);
            }
        }
    }

    public int valor(int l, int c) throws JogadaInvalidaException {
        if (matriz == null) {
            throw new JogadaInvalidaException("A partida ainda não começou.");
        }
        if (l < 0 || l >= linhas || c < 0 || c >= colunas) {
            throw new JogadaInvalidaException("Posição inválida.");
        }
        return matriz[l][c];
    }

    public int getLinhas() {
        return linhas;
    }

    public int getColunas() {
        return colunas;
    }

    // TODO: vez do jogador, acertos/erros, comparar as duas cartas, fim de jogo

    // Matriz sorteada em texto (para mostrar no console do servidor)
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

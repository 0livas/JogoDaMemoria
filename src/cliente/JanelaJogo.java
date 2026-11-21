package cliente;

import comum.IServidorJogo;
import comum.JogadaInvalidaException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Interface gráfica (Swing) do jogo
public class JanelaJogo extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final int TAMANHO_CARTA = 96;

    private final transient IServidorJogo servidor;
    // As chamadas RMI saem desta thread para não travar a interface
    private final transient ExecutorService chamadasRmi = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chamadas-rmi");
        t.setDaemon(true);
        return t;
    });

    private final JLabel lblMensagem = new JLabel("Conectando...");
    private final JPanel painelTabuleiro = new JPanel();
    private JButton[][] botoes;
    private int meuId = -1;
    private boolean[][] viradas;

    public JanelaJogo(IServidorJogo servidor, Runnable aoFechar) {
        super("Jogo da Memória — RMI");
        this.servidor = servidor;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                aoFechar.run();
            }
        });

        JPanel raiz = new JPanel(new BorderLayout(10, 10));
        raiz.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(raiz);

        painelTabuleiro.setOpaque(false);
        raiz.add(painelTabuleiro, BorderLayout.CENTER);

        lblMensagem.setFont(lblMensagem.getFont().deriveFont(14f));
        raiz.add(lblMensagem, BorderLayout.SOUTH);

        // TODO: placar (acertos/erros) e indicador de quem tem a vez
    }

    public void definirJogador(int id, String nome) {
        this.meuId = id;
        setTitle("Jogo da Memória — " + nome + " (Jogador " + (id + 1) + ")");
    }

    // Callback: alguém (eu ou o adversário) revelou uma carta
    public void mostrarCarta(int linha, int coluna, int valor, String jogador) {
        if (botoes == null) return;
        viradas[linha][coluna] = true;
        botoes[linha][coluna].setIcon(Imagens.carta(valor, TAMANHO_CARTA));
        lblMensagem.setText(jogador + " revelou a carta (" + (linha + 1) + "," + (coluna + 1) + ")");
    }

    // Callback: alguém escondeu uma carta
    public void esconderCarta(int linha, int coluna, String jogador) {
        if (botoes == null) return;
        viradas[linha][coluna] = false;
        botoes[linha][coluna].setIcon(Imagens.verso(TAMANHO_CARTA));
        lblMensagem.setText(jogador + " escondeu a carta (" + (linha + 1) + "," + (coluna + 1) + ")");
    }

    // Recebe os avisos do servidor (callback)
    public void mostrarMensagem(String mensagem) {
        lblMensagem.setText(mensagem);
    }

    public void construirTabuleiro(int linhas, int colunas) {
        painelTabuleiro.removeAll();
        painelTabuleiro.setLayout(new GridLayout(linhas, colunas, 6, 6));
        botoes = new JButton[linhas][colunas];
        viradas = new boolean[linhas][colunas];
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                JButton b = new JButton(Imagens.verso(TAMANHO_CARTA));
                b.setPreferredSize(new Dimension(TAMANHO_CARTA + 6, TAMANHO_CARTA + 6));
                b.setFocusPainted(false);
                b.setContentAreaFilled(false);
                b.setOpaque(true);
                b.setBackground(Color.WHITE);
                final int linha = l, coluna = c;
                b.addActionListener(e -> clicarCarta(linha, coluna));
                botoes[l][c] = b;
                painelTabuleiro.add(b);
            }
        }
        painelTabuleiro.revalidate();
    }

    private void clicarCarta(int linha, int coluna) {
        boolean virada = viradas[linha][coluna];
        // Só pede ao servidor (chamada RMI); a tela muda quando chega o callback,
        // assim os dois clientes atualizam juntos
        chamadasRmi.submit(() -> {
            try {
                if (virada) {
                    servidor.ocultarCarta(meuId, linha, coluna);
                } else {
                    servidor.revelarCarta(meuId, linha, coluna);
                }
            } catch (JogadaInvalidaException ex) {
                SwingUtilities.invokeLater(() -> lblMensagem.setText(ex.getMessage()));
            } catch (RemoteException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Conexão com o servidor perdida:\n" + ex.getMessage(),
                        "Erro de comunicação", JOptionPane.ERROR_MESSAGE));
            }
        });
        // TODO: comparar as duas cartas, acerto mantém a vez, erro esconde e passa a vez
    }
}

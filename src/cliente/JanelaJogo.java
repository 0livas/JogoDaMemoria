package cliente;

import comum.IServidorJogo;
import comum.EstadoJogo;
import comum.JogadaInvalidaException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JToggleButton;
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
    private EstadoJogo estado;
    private final JToggleButton btnRevanche = new JToggleButton("Jogar de novo");
    private final JLabel lblRevanche = new JLabel(" ");
    private final JLabel lblPlacar = new JLabel(" ");

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
        JPanel rodape = new JPanel(new BorderLayout(6, 6));
        JPanel revanche = new JPanel(new BorderLayout(6, 6));
        revanche.add(btnRevanche, BorderLayout.WEST);
        revanche.add(lblRevanche, BorderLayout.CENTER);
        rodape.add(lblMensagem, BorderLayout.NORTH);
        rodape.add(revanche, BorderLayout.SOUTH);
        raiz.add(rodape, BorderLayout.SOUTH);
        btnRevanche.setEnabled(false);
        btnRevanche.addActionListener(e -> solicitarRevanche(btnRevanche.isSelected()));

        raiz.add(lblPlacar, BorderLayout.NORTH);
    }

    public void definirJogador(int id, String nome) {
        this.meuId = id;
        atualizarBotoes();
        setTitle("Jogo da Memória — " + nome + " (Jogador " + (id + 1) + ")");
    }

    // O cliente apenas apresenta o estado decidido pelo servidor.
    public void atualizarEstado(EstadoJogo novo) {
        if (estado != null && novo.versao <= estado.versao) return;
        estado = novo;
        if (botoes == null) construirTabuleiro(novo.cartas.length, novo.cartas[0].length);
        for (int l = 0; l < botoes.length; l++)
            for (int c = 0; c < botoes[l].length; c++) {
                javax.swing.ImageIcon imagem = novo.cartas[l][c] < 0 ? Imagens.verso(TAMANHO_CARTA)
                        : Imagens.carta(novo.cartas[l][c], TAMANHO_CARTA);
                botoes[l][c].setIcon(imagem);
                botoes[l][c].setDisabledIcon(imagem);
            }
        lblMensagem.setText(novo.mensagem);
        lblPlacar.setText(placar(0) + "    |    " + placar(1));
        atualizarBotoes();
    }

    private String placar(int id) {
        String nome = estado.nomes[id] == null ? "Aguardando jogador" : estado.nomes[id];
        return nome + ": " + estado.acertos[id] + " pares / " + estado.erros[id] + " erros";
    }

    private void atualizarBotoes() {
        if (estado == null || botoes == null) return;
        boolean jogadorValido = meuId >= 0 && meuId < 2;
        btnRevanche.setEnabled(jogadorValido && estado.encerrada);
        btnRevanche.setSelected(jogadorValido && estado.revanche[meuId]);
        btnRevanche.setText(btnRevanche.isSelected() ? "Cancelar revanche" : "Jogar de novo");
        int confirmados = (estado.revanche[0] ? 1 : 0) + (estado.revanche[1] ? 1 : 0);
        lblRevanche.setText(estado.encerrada ? "Revanche: " + confirmados + "/2 confirmados" : " ");
        boolean minhaVez = estado.ativa && !estado.aguardandoOcultar && estado.vez == meuId;
        for (int l = 0; l < botoes.length; l++)
            for (int c = 0; c < botoes[l].length; c++)
                botoes[l][c].setEnabled(minhaVez && estado.cartas[l][c] < 0);
    }

    public void construirTabuleiro(int linhas, int colunas) {
        painelTabuleiro.removeAll();
        painelTabuleiro.setLayout(new GridLayout(linhas, colunas, 6, 6));
        botoes = new JButton[linhas][colunas];
        for (int l = 0; l < linhas; l++) {
            for (int c = 0; c < colunas; c++) {
                JButton b = new JButton(Imagens.verso(TAMANHO_CARTA));
                b.setPreferredSize(new Dimension(TAMANHO_CARTA + 6, TAMANHO_CARTA + 6));
                b.setEnabled(false);
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

    private void solicitarRevanche(boolean aceita) {
        btnRevanche.setEnabled(false);
        chamadasRmi.submit(() -> {
            try {
                servidor.definirRevanche(meuId, aceita);
            } catch (JogadaInvalidaException | RemoteException ex) {
                SwingUtilities.invokeLater(() -> {
                    lblMensagem.setText(ex.getMessage());
                    atualizarBotoes();
                });
            }
        });
    }

    private void clicarCarta(int linha, int coluna) {
        // Só pede ao servidor (chamada RMI); a tela muda quando chega o callback,
        // assim os dois clientes atualizam juntos
        chamadasRmi.submit(() -> {
            try {
                servidor.revelarCarta(meuId, linha, coluna);
            } catch (JogadaInvalidaException ex) {
                SwingUtilities.invokeLater(() -> lblMensagem.setText(ex.getMessage()));
            } catch (RemoteException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Conexão com o servidor perdida:\n" + ex.getMessage(),
                        "Erro de comunicação", JOptionPane.ERROR_MESSAGE));
            }
        });
    }
}

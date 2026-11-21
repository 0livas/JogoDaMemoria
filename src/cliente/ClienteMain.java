package cliente;

import comum.IServidorJogo;
import comum.JogadaInvalidaException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GridLayout;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicReference;

/**
 Cliente do jogo
 */
public class ClienteMain {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // usa o visual padrão
        }

        String host = args.length > 0 ? args[0] : "";
        String nome = args.length > 1 ? args[1] : System.getProperty("user.name", "Jogador");
        String porta = args.length > 2 ? args[2] : String.valueOf(IServidorJogo.PORTA_PADRAO);

        if (args.length < 2) {
            String[] dados = pedirDados(host.isEmpty() ? "localhost" : host, nome, porta);
            if (dados == null) return;
            host = dados[0];
            nome = dados[1];
            porta = dados[2];
        }

        try {
            conectar(host.trim(), Integer.parseInt(porta.trim()), nome.trim());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Não foi possível conectar ao servidor " + host + ":" + porta + "\n\n" + e,
                    "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void conectar(String host, int porta, String nome) throws Exception {
        String ipLocal = descobrirIpLocal(host, porta);
        System.setProperty("java.rmi.server.hostname", ipLocal);
        System.out.println("IP local usado no callback: " + ipLocal);

        // 1) Localiza o registry e obtém o STUB do servidor
        Registry registry = LocateRegistry.getRegistry(host, porta);
        IServidorJogo servidor = (IServidorJogo) registry.lookup(IServidorJogo.NOME_REGISTRO);
        System.out.println("Stub obtido: " + servidor);

        // 2) Cria a janela e o objeto de callback (exportado na porta indicada ou em uma livre)
        AtomicReference<JanelaJogo> ref = new AtomicReference<>();
        AtomicReference<ClienteCallbackImpl> callbackRef = new AtomicReference<>();
        AtomicReference<Integer> idRef = new AtomicReference<>(-1);

        Runnable aoFechar = () -> {
            try {
                if (idRef.get() >= 0) servidor.sair(idRef.get()); // chamada RMI
            } catch (Exception ignorada) {
                // servidor pode já estar fora do ar
            }
            try {
                if (callbackRef.get() != null) UnicastRemoteObject.unexportObject(callbackRef.get(), true);
            } catch (Exception ignorada) {
                // nada a fazer
            }
            System.exit(0);
        };

        SwingUtilities.invokeAndWait(() -> ref.set(new JanelaJogo(servidor, aoFechar)));
        JanelaJogo janela = ref.get();

        int portaCallback = Integer.getInteger("porta.callback", 0);
        ClienteCallbackImpl callback = new ClienteCallbackImpl(portaCallback, janela);
        callbackRef.set(callback);

        // 3) Chamada remota: entra na partida enviando o stub do callback
        int id;
        try {
            id = servidor.entrar(nome, callback);
        } catch (JogadaInvalidaException e) {
            UnicastRemoteObject.unexportObject(callback, true);
            JOptionPane.showMessageDialog(null, e.getMessage(), "Não foi possível entrar", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
            return;
        }
        idRef.set(id);

        int[] tamanho = servidor.obterTamanho(); // chamada RMI
        SwingUtilities.invokeLater(() -> {
            janela.construirTabuleiro(tamanho[0], tamanho[1]);
            janela.definirJogador(id, nome);
            janela.pack();
            janela.setLocationByPlatform(true);
            janela.setVisible(true);
        });
    }

    private static String descobrirIpLocal(String host, int porta) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, porta), 5000);
            return s.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            throw new IllegalStateException("Servidor " + host + ":" + porta + " inacessível (" + e.getMessage() + ")", e);
        }
    }

    private static String[] pedirDados(String host, String nome, String porta) {
        JTextField txtHost = new JTextField(host, 18);
        JTextField txtNome = new JTextField(nome, 18);
        JTextField txtPorta = new JTextField(porta, 18);
        JPanel painel = new JPanel(new GridLayout(0, 2, 6, 6));
        painel.add(new JLabel("IP do servidor:"));
        painel.add(txtHost);
        painel.add(new JLabel("Seu nome:"));
        painel.add(txtNome);
        painel.add(new JLabel("Porta do registry:"));
        painel.add(txtPorta);
        int op = JOptionPane.showConfirmDialog(null, painel, "Jogo da Memória — conectar",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return null;
        return new String[]{txtHost.getText(), txtNome.getText(), txtPorta.getText()};
    }
}

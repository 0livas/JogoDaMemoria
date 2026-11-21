package cliente;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Imagens das cartas, armazenadas local no cliente 
 */
final class Imagens {

    private static final Map<String, ImageIcon> CACHE = new HashMap<>();

    private Imagens() {
    }

    static ImageIcon carta(int valor, int tamanho) {
        return carregar(String.format("/imagens/carta_%02d.png", valor), tamanho, valor);
    }

    static ImageIcon verso(int tamanho) {
        return carregar("/imagens/verso.png", tamanho, -1);
    }

    private static synchronized ImageIcon carregar(String caminho, int tamanho, int valorAlternativo) {
        String chave = caminho + "@" + tamanho;
        return CACHE.computeIfAbsent(chave, k -> {
            BufferedImage original = null;
            try (InputStream in = Imagens.class.getResourceAsStream(caminho)) {
                if (in != null) original = ImageIO.read(in);
            } catch (Exception ignorada) {
                // usa a imagem alternativa abaixo
            }
            if (original == null) original = desenharAlternativa(valorAlternativo);
            return new ImageIcon(redimensionar(original, tamanho));
        });
    }

    private static BufferedImage redimensionar(BufferedImage img, int tamanho) {
        BufferedImage saida = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = saida.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, tamanho, tamanho, null);
        g.dispose();
        return saida;
    }

    // Usada só se a imagem não existir no jar
    private static BufferedImage desenharAlternativa(int valor) {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(valor < 0 ? new Color(30, 60, 110) : Color.getHSBColor((valor * 0.13f) % 1f, 0.6f, 0.9f));
        g.fillRoundRect(0, 0, 200, 200, 30, 30);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 90));
        String texto = valor < 0 ? "?" : String.valueOf(valor);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(texto, (200 - fm.stringWidth(texto)) / 2, (200 + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }
}

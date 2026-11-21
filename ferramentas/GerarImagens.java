import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;


public class GerarImagens {
    static final int T = 200;
    static final int QTD = 18;

    public static void main(String[] args) throws Exception {
        File pasta = new File(args.length > 0 ? args[0] : "recursos/imagens");
        pasta.mkdirs();
        for (int i = 0; i < QTD; i++) {
            ImageIO.write(carta(i), "png", new File(pasta, String.format("carta_%02d.png", i)));
        }
        ImageIO.write(verso(), "png", new File(pasta, "verso.png"));
        System.out.println("Imagens geradas em " + pasta.getAbsolutePath());
    }

    static BufferedImage carta(int i) {
        BufferedImage img = novaImagem();
        Graphics2D g = graficos(img);
        float matiz = ((i * 7) % QTD) / (float) QTD;
        Color cor = Color.getHSBColor(matiz, 0.65f, 0.88f);
        g.setPaint(new GradientPaint(0, 0, cor.brighter(), T, T, cor.darker()));
        g.fill(new RoundRectangle2D.Double(0, 0, T, T, 36, 36));

        g.setColor(Color.WHITE);
        g.fill(forma(i % 6, T / 2.0, T / 2.0 + 6, 62));
        g.setColor(new Color(0, 0, 0, 60));
        g.setStroke(new BasicStroke(4));
        g.draw(forma(i % 6, T / 2.0, T / 2.0 + 6, 62));

        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        g.drawString(String.valueOf(i), 16, 44);
        g.dispose();
        return img;
    }

    static BufferedImage verso() {
        BufferedImage img = novaImagem();
        Graphics2D g = graficos(img);
        g.setPaint(new GradientPaint(0, 0, new Color(40, 80, 150), T, T, new Color(20, 35, 80)));
        g.fill(new RoundRectangle2D.Double(0, 0, T, T, 36, 36));
        g.setColor(new Color(255, 255, 255, 35));
        g.setStroke(new BasicStroke(6));
        for (int d = -T; d < T * 2; d += 28) g.drawLine(d, 0, d - T, T);
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(5));
        g.draw(new RoundRectangle2D.Double(14, 14, T - 28, T - 28, 24, 24));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 110));
        FontMetrics fm = g.getFontMetrics();
        g.drawString("?", (T - fm.stringWidth("?")) / 2, (T + fm.getAscent() - fm.getDescent()) / 2);
        g.dispose();
        return img;
    }

    static Shape forma(int tipo, double cx, double cy, double r) {
        switch (tipo) {
            case 0: return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
            case 1: return new RoundRectangle2D.Double(cx - r * 0.85, cy - r * 0.85, r * 1.7, r * 1.7, 20, 20);
            case 2: return poligono(3, cx, cy, r * 1.1, -Math.PI / 2);
            case 3: return poligono(4, cx, cy, r, -Math.PI / 2);
            case 4: return estrela(cx, cy, r * 1.1, r * 0.45);
            default: return poligono(6, cx, cy, r, 0);
        }
    }

    static Shape poligono(int lados, double cx, double cy, double r, double inicio) {
        Polygon p = new Polygon();
        for (int k = 0; k < lados; k++) {
            double a = inicio + 2 * Math.PI * k / lados;
            p.addPoint((int) Math.round(cx + r * Math.cos(a)), (int) Math.round(cy + r * Math.sin(a)));
        }
        return p;
    }

    static Shape estrela(double cx, double cy, double rExt, double rInt) {
        Path2D p = new Path2D.Double();
        for (int k = 0; k < 10; k++) {
            double r = k % 2 == 0 ? rExt : rInt;
            double a = -Math.PI / 2 + Math.PI * k / 5;
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    static BufferedImage novaImagem() {
        return new BufferedImage(T, T, BufferedImage.TYPE_INT_ARGB);
    }

    static Graphics2D graficos(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }
}

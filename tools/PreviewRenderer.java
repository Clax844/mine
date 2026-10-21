import fr.clax.aurel.ui.Canvas;
import fr.clax.aurel.ui.MenuSettings;
import fr.clax.aurel.ui.MenuUi;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Offscreen rendering of the shared UI, not a screenshot or validation of Minecraft. */
public final class PreviewRenderer implements Canvas {
    private final Graphics2D graphics;
    private PreviewRenderer(Graphics2D graphics) { this.graphics = graphics; }

    public static void main(String[] args) throws Exception {
        Path output = Path.of(args.length == 0 ? "build/preview.png" : args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        BufferedImage image = new BufferedImage(1500, 990, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x132a38), 1500, 990, new Color(0x080f1b)));
        g.fillRect(0, 0, 1500, 990);
        g.translate(75, 60); g.scale(1.5, 1.5);
        PreviewRenderer canvas = new PreviewRenderer(g);
        MenuUi ui = new MenuUi(new MenuSettings(), _ -> {}, () -> {});
        ui.paint(canvas, -1, -1, .016, "Clax844");
        if (args.length > 1) {
            ui.mouseDown(80, 160 + Integer.parseInt(args[1]) * 44);
            ui.paint(canvas, -1, -1, 1, "Clax844");
        }
        g.dispose(); ImageIO.write(image, "png", output.toFile());
        System.out.println(output.toAbsolutePath());
    }

    public void fill(int x, int y, int width, int height, int argb) {
        // Adjacent pixel bands must share edges when the offscreen image is scaled.
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.setColor(new Color(argb, true)); graphics.fillRect(x, y, width, height);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    public void text(String text, int x, int y, int size, int argb) {
        graphics.setFont(new Font("SansSerif", Font.PLAIN, size)); graphics.setColor(new Color(argb, true));
        graphics.drawString(text, x, y + graphics.getFontMetrics().getAscent());
    }
    public int textWidth(String text, int size) {
        graphics.setFont(new Font("SansSerif", Font.PLAIN, size)); return graphics.getFontMetrics().stringWidth(text);
    }
    public void avatar(int x, int y, int width, int height) {
        // A neutral silhouette for the standalone preview. In game this is the real player model.
        int unit = Math.max(2, height / 8), center = x + width / 2;
        round(center - unit, y + unit / 2, unit * 2, unit * 2, 2, 0xff9bb5ca);
        round(center - unit, y + unit * 3, unit * 2, unit * 2, 2, 0xff4e91b4);
        fill(center - unit, y + unit * 5, unit - 1, unit * 2, 0xff42546e);
        fill(center + 1, y + unit * 5, unit - 1, unit * 2, 0xff42546e);
    }
}

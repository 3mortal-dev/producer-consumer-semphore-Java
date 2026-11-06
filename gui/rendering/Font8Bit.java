package gui.rendering;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.FontFormatException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for 8-bit arcade font (Press Start 2P).
 * Loads TTF from the classpath, then from the source tree as a fallback.
 */
public final class Font8Bit {
    private static Font baseFont;
    private static final String FONT_RESOURCE = "/fonts/PressStart2P-Regular.ttf";
    private static final String FONT_FILE = "resources/fonts/PressStart2P-Regular.ttf";

    static {
        baseFont = loadFont();
    }

    private static Font loadFont() {
        // 1) classpath resource (e.g. run with -cp out:resources)
        try (InputStream is = Font8Bit.class.getResourceAsStream(FONT_RESOURCE)) {
            if (is != null) {
                return Font.createFont(Font.TRUETYPE_FONT, is);
            }
        } catch (FontFormatException | IOException e) {
            System.err.println("Classpath font load failed: " + e.getMessage());
        }

        // 2) filesystem fallback relative to the working directory
        File file = new File(FONT_FILE);
        if (file.isFile()) {
            try {
                return Font.createFont(Font.TRUETYPE_FONT, file);
            } catch (FontFormatException | IOException e) {
                System.err.println("File font load failed: " + e.getMessage());
            }
        }

        // 3) generic fallback
        System.err.println("8-bit font not found; using Monospaced fallback");
        return new Font("Monospaced", Font.PLAIN, 8);
    }

    private Font8Bit() {}

    /**
     * Get font at specified pixel size (will be scaled by render scale).
     */
    public static Font get(int pixelSize) {
        return baseFont.deriveFont(Font.PLAIN, pixelSize);
    }

    /**
     * Draw string at (x, y) in pixels (unscaled coordinates).
     * Assumes Graphics2D has scale transform applied if needed.
     */
    public static void drawString(Graphics2D g, String text, int x, int y, int pixelSize, java.awt.Color color) {
        Font oldFont = g.getFont();
        g.setFont(get(pixelSize));
        g.setColor(color);
        g.drawString(text, x, y);
        g.setFont(oldFont);
    }

    /**
     * Draw string centered horizontally at given y.
     */
    public static void drawCentered(Graphics2D g, String text, int centerX, int y, int pixelSize, java.awt.Color color) {
        Font oldFont = g.getFont();
        Font font = get(pixelSize);
        g.setFont(font);
        g.setColor(color);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        g.drawString(text, x, y);
        g.setFont(oldFont);
    }

    /**
     * Draw string right-aligned at given x, y.
     */
    public static void drawRightAligned(Graphics2D g, String text, int rightX, int y, int pixelSize, java.awt.Color color) {
        Font oldFont = g.getFont();
        Font font = get(pixelSize);
        g.setFont(font);
        g.setColor(color);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int x = rightX - fm.stringWidth(text);
        g.drawString(text, x, y);
        g.setFont(oldFont);
    }

    /**
     * Get string width in pixels at given size.
     */
    public static int stringWidth(Graphics2D g, String text, int pixelSize) {
        Font font = get(pixelSize);
        return g.getFontMetrics(font).stringWidth(text);
    }

    /**
     * Get font height in pixels at given size.
     */
    public static int fontHeight(Graphics2D g, int pixelSize) {
        Font font = get(pixelSize);
        return g.getFontMetrics(font).getHeight();
    }
}
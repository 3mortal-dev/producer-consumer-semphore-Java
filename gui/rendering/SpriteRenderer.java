package gui.rendering;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Utility for drawing sprites from the atlas with scaling and animation support.
 */
public final class SpriteRenderer {
    private SpriteRenderer() {}

    /**
     * Draw sprite at pixel coordinates (unscaled, logical coordinates).
     * The canvas will scale the entire buffer.
     */
    public static void draw(Graphics2D g, BufferedImage sprite, int x, int y) {
        if (sprite != null) {
            g.drawImage(sprite, x, y, null);
        }
    }

    /**
     * Draw sprite with horizontal flip.
     */
    public static void drawFlipped(Graphics2D g, BufferedImage sprite, int x, int y, boolean flipX) {
        if (sprite == null) return;
        if (flipX) {
            g.drawImage(sprite, x + sprite.getWidth(), y, -sprite.getWidth(), sprite.getHeight(), null);
        } else {
            g.drawImage(sprite, x, y, null);
        }
    }

    /**
     * Draw sprite scaled by factor (for UI elements that need different sizes).
     */
    public static void drawScaled(Graphics2D g, BufferedImage sprite, int x, int y, int scale) {
        if (sprite != null) {
            int w = sprite.getWidth() * scale;
            int h = sprite.getHeight() * scale;
            g.drawImage(sprite, x, y, w, h, null);
        }
    }

    /**
     * Draw a colored rectangle (for debug/placeholder).
     */
    public static void drawRect(Graphics2D g, int x, int y, int w, int h, java.awt.Color color) {
        g.setColor(color);
        g.fillRect(x, y, w, h);
    }

    /**
     * Draw outline rectangle.
     */
    public static void drawRectOutline(Graphics2D g, int x, int y, int w, int h, java.awt.Color color) {
        g.setColor(color);
        g.drawRect(x, y, w - 1, h - 1);
    }
}
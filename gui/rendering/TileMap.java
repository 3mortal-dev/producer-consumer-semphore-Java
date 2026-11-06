package gui.rendering;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Static tile map for the hospital floor corridor that connects the waiting
 * room (top) to the two treatment rooms (bottom). Everything is drawn in
 * logical pixels; the canvas scales the whole buffer with nearest-neighbour.
 */
public final class TileMap {
    private TileMap() {}

    public static final int FLOOR_X = 16;
    public static final int FLOOR_Y = 248;
    public static final int FLOOR_W = 992;
    public static final int FLOOR_H = 244;

    public static final int TILE = 16;
    public static final int CORRIDOR_Y = FLOOR_Y + FLOOR_H / 2;
    public static final int TOP_DOOR_X = FLOOR_X + FLOOR_W / 2;
    public static final int ROOM1_DOOR_X = 164;
    public static final int ROOM2_DOOR_X = 338;
    public static final int EXIT_X = FLOOR_X + 24;

    private static final Color FLOOR_A = new Color(0x3A, 0x3A, 0x4A);
    private static final Color FLOOR_B = new Color(0x2E, 0x2E, 0x3C);
    private static final Color FLOOR_LINE = new Color(0x24, 0x24, 0x30);
    private static final int DOOR_GAP = 52;

    public static void drawFloor(Graphics2D g) {
        int x = FLOOR_X, y = FLOOR_Y, w = FLOOR_W, h = FLOOR_H;

        // Outer wall
        g.setColor(ColorPalette.BLUE_DARK);
        g.fillRect(x, y, w, h);

        // Checkerboard tiles (inset so a wall frame remains)
        int fx = x + 8, fy = y + 8, fw = w - 16, fh = h - 16;
        for (int ty = 0; ty < fh; ty += TILE) {
            for (int tx = 0; tx < fw; tx += TILE) {
                boolean even = ((tx / TILE) + (ty / TILE)) % 2 == 0;
                g.setColor(even ? FLOOR_A : FLOOR_B);
                g.fillRect(fx + tx, fy + ty, Math.min(TILE, fw - tx), Math.min(TILE, fh - ty));
            }
        }

        // Faint tile grid
        g.setColor(FLOOR_LINE);
        for (int tx = 0; tx <= fw; tx += TILE) {
            g.drawLine(fx + tx, fy, fx + tx, fy + fh);
        }
        for (int ty = 0; ty <= fh; ty += TILE) {
            g.drawLine(fx, fy + ty, fx + fw, fy + ty);
        }

        // Floor chevrons leading from the waiting-room door to the treatment rooms
        drawChevrons(g, TOP_DOOR_X, fy + 24, CORRIDOR_Y - 24, ColorPalette.BLUE_LIGHT);
        drawChevrons(g, ROOM1_DOOR_X, CORRIDOR_Y + 18, fy + fh - 26, ColorPalette.GRAY_MED);
        drawChevrons(g, ROOM2_DOOR_X, CORRIDOR_Y + 18, fy + fh - 26, ColorPalette.GRAY_MED);

        // Faded red cross emblem in the middle of the corridor
        drawFadedCross(g, x + w / 2 - 22, CORRIDOR_Y - 22);

        // Wall trim
        g.setColor(ColorPalette.METAL);
        g.fillRect(x, y, w, 4);
        g.fillRect(x, y + h - 4, w, 4);
        g.setColor(ColorPalette.GRAY_DARK);
        g.drawRect(x, y, w - 1, h - 1);

        // Doorways (cut the trim and mark thresholds)
        drawDoorway(g, TOP_DOOR_X, y, true);
        drawDoorway(g, ROOM1_DOOR_X, y + h - 4, false);
        drawDoorway(g, ROOM2_DOOR_X, y + h - 4, false);
        drawSideExit(g, x, CORRIDOR_Y);

        // Signs
        drawSign(g, "WAITING ROOM", TOP_DOOR_X, y + 20, true);
        drawSign(g, "TREATMENT ROOMS", (ROOM1_DOOR_X + ROOM2_DOOR_X) / 2, y + h - 26, false);

        // Header tab
        g.setColor(ColorPalette.BLACK);
        g.fillRect(x + 2, y + 2, 128, 14);
        Font8Bit.drawString(g, "HOSPITAL FLOOR", x + 6, y + 13, 7, ColorPalette.YELLOW);

        // Decorations
        drawPlant(g, x + 16, CORRIDOR_Y - 34);
        drawPlant(g, x + w - 32, CORRIDOR_Y - 34);
        drawGurney(g, x + w - 120, CORRIDOR_Y - 8);
    }

    private static void drawChevrons(Graphics2D g, int centerX, int fromY, int toY, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(2f));
        int dir = toY >= fromY ? 1 : -1;
        for (int yy = fromY; dir > 0 ? yy <= toY : yy >= toY; yy += 18) {
            g.drawLine(centerX - 6, yy, centerX, yy + 6 * dir);
            g.drawLine(centerX, yy + 6 * dir, centerX + 6, yy);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private static void drawFadedCross(Graphics2D g, int x, int y) {
        g.setColor(new Color(216, 32, 16, 40));
        g.fillRect(x + 14, y, 10, 44);
        g.fillRect(x, y + 16, 44, 10);
    }

    private static void drawDoorway(Graphics2D g, int centerX, int wallY, boolean top) {
        int left = centerX - DOOR_GAP / 2;
        // Cut the wall trim
        g.setColor(top ? FLOOR_A : FLOOR_B);
        g.fillRect(left, wallY, DOOR_GAP, 5);
        // Door frame posts
        g.setColor(ColorPalette.GRAY_MED);
        g.fillRect(left - 3, wallY - (top ? 0 : 4), 3, top ? 10 : 12);
        g.fillRect(left + DOOR_GAP, wallY - (top ? 0 : 4), 3, top ? 10 : 12);
        // Threshold
        g.setColor(ColorPalette.YELLOW);
        g.fillRect(left + 2, wallY + (top ? 3 : 0), DOOR_GAP - 4, 2);
    }

    private static void drawSideExit(Graphics2D g, int wallX, int centerY) {
        g.setColor(FLOOR_A);
        g.fillRect(wallX, centerY - 14, 5, 28);
        g.setColor(ColorPalette.GRAY_MED);
        g.fillRect(wallX, centerY - 17, 3, 3);
        g.fillRect(wallX, centerY + 14, 3, 3);
        g.setColor(ColorPalette.YELLOW);
        g.fillRect(wallX + 3, centerY - 12, 2, 24);
    }

    private static void drawSign(Graphics2D g, String text, int centerX, int y, boolean pointingUp) {
        int w = Font8Bit.stringWidth(g, text, 6) + 12;
        int x = centerX - w / 2;
        g.setColor(ColorPalette.BLACK);
        g.fillRect(x, y - 10, w, 13);
        g.setColor(ColorPalette.BLUE_LIGHT);
        g.drawRect(x, y - 10, w - 1, 12);
        Font8Bit.drawString(g, text, x + 6, y, 6, ColorPalette.WHITE);

        // Small arrow
        g.setColor(ColorPalette.YELLOW);
        int ax = centerX - 3;
        if (pointingUp) {
            g.fillPolygon(new int[]{ax, ax + 3, ax + 6}, new int[]{y + 8, y + 3, y + 8}, 3);
        } else {
            g.fillPolygon(new int[]{ax, ax + 3, ax + 6}, new int[]{y + 3, y + 8, y + 3}, 3);
        }
    }

    private static void drawPlant(Graphics2D g, int x, int y) {
        // Pot
        g.setColor(ColorPalette.BROWN);
        g.fillRect(x + 3, y + 12, 10, 6);
        // Leaves
        g.setColor(ColorPalette.GREEN);
        g.fillRect(x + 6, y + 2, 4, 10);
        g.fillRect(x + 2, y + 6, 4, 4);
        g.fillRect(x + 10, y + 5, 4, 5);
        g.setColor(new Color(0x2A, 0x8A, 0x2A));
        g.fillRect(x + 5, y + 4, 2, 3);
    }

    private static void drawGurney(Graphics2D g, int x, int y) {
        g.setColor(ColorPalette.METAL);
        g.fillRect(x, y, 28, 4);          // frame
        g.fillRect(x + 2, y + 4, 2, 8);   // legs
        g.fillRect(x + 24, y + 4, 2, 8);
        g.setColor(ColorPalette.BED_SHEET);
        g.fillRect(x + 1, y - 7, 26, 7); // mattress
        g.setColor(ColorPalette.WHITE);
        g.fillRect(x + 2, y - 7, 8, 4);  // pillow
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(x + 28, y - 6, 2, 10); // handle
    }
}
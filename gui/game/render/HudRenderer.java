package gui.game.render;

import gui.game.iso.IsoProjection;
import gui.game.scene.SceneId;
import gui.game.world.WorldState;
import gui.rendering.Font8Bit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Fixed-resolution arcade HUD. It exposes the clickable rectangles so the
 * canvas can route mouse input without duplicating layout math.
 */
public final class HudRenderer {
    // Grown from 36/16 so the minimum readable text size (8) fits comfortably.
    public static final int TOP_H = 44;
    public static final int BOTTOM_Y = 340;
    public static final int BOTTOM_H = 20;

    public static final float[] SPEEDS = {0.5f, 1.0f, 2.0f, 4.0f};
    private static final String[] SPEED_LABELS = {"0.5x", "1x", "2x", "4x"};

    private static final Color HUD_BG = new Color(0x03, 0x04, 0x08);
    private static final Color HUD_ROW = new Color(0x06, 0x07, 0x0D);
    private static final Color HUD_LINE = new Color(0x1E, 0x2C, 0x48);
    private static final Color TAB_BG = new Color(0x0C, 0x10, 0x1C);
    private static final Color TAB_ACTIVE = new Color(0x1B, 0x42, 0x72);
    private static final Color TAB_BORDER = new Color(0x28, 0x36, 0x56);
    private static final Color TEXT_DIM = new Color(0x9C, 0xA8, 0xC0);
    private static final Color TEXT_SOFT = new Color(0xC8, 0xD4, 0xE8);
    private static final Color ACCENT = new Color(0x54, 0xD8, 0xF0);
    private static final Color GOOD = new Color(0x6C, 0xE0, 0x80);
    private static final Color WARN = new Color(0xE8, 0xCC, 0x54);
    private static final Color BAD = new Color(0xF0, 0x5A, 0x4A);

    private final Rectangle[] tabRects = new Rectangle[SceneId.values().length];
    private final Rectangle[] speedRects = new Rectangle[SPEEDS.length];
    private final Rectangle pauseRect = new Rectangle();
    private final Rectangle eventsRect = new Rectangle();

    private SceneId hoverTab;
    private int hoverSpeed = -1;
    private boolean hoverPause;
    private boolean hoverEvents;

    private final BufferedImage measureImage =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private final Graphics2D measureGraphics = measureImage.createGraphics();

    public HudRenderer() {
        layout();
    }

    /** Recomputes clickable rectangles for the current font mode. Call after
     *  {@code Font8Bit.setMode(...)} so tabs/buttons fit the new glyph widths. */
    public void setFontMode(Font8Bit.FontMode mode) {
        Font8Bit.setMode(mode);
        layout();
    }

    private void layout() {
        int x = 4;
        int y = 2;
        for (SceneId scene : SceneId.values()) {
            String label = "[" + scene.label + "]";
            int w = measure(label, 8) + 10;
            tabRects[scene.ordinal()] = new Rectangle(x, y, w, 18);
            x += w + 2;
        }

        int right = 636;
        int eventsW = measure("EVENTS:OFF", 8) + 8;
        eventsRect.setBounds(right - eventsW, 3, eventsW, 15);
        right -= eventsW + 3;

        int pauseW = measure("RESUME", 8) + 8;
        pauseRect.setBounds(right - pauseW, 3, pauseW, 15);
        right -= pauseW + 3;

        for (int i = SPEEDS.length - 1; i >= 0; i--) {
            int w = measure(SPEED_LABELS[i], 8) + 8;
            speedRects[i] = new Rectangle(right - w, 3, w, 15);
            right -= w + 2;
        }
    }

    // ------------------------------------------------------------------
    // Main drawing
    // ------------------------------------------------------------------

    public void draw(Graphics2D g, WorldState world, SceneId scene,
                     float speed, boolean paused, boolean toastsEnabled) {
        draw(g, world, scene, speed, paused, toastsEnabled, "");
    }

    /**
     * @param pacingText short playback status for the bottom strip, e.g.
     *                   "STEP: PRESS N (QUEUED 12)" or "DELAY 0.7S QUEUED 3".
     *                   Empty when pacing is off.
     */
    public void draw(Graphics2D g, WorldState world, SceneId scene,
                     float speed, boolean paused, boolean toastsEnabled,
                     String pacingText) {
        g.setFont(Font8Bit.get(8));
        g.setStroke(new BasicStroke(1f));

        // Top two-row HUD.
        g.setColor(HUD_BG);
        g.fillRect(0, 0, IsoProjection.VIEW_W, TOP_H);
        g.setColor(HUD_ROW);
        g.fillRect(0, 21, IsoProjection.VIEW_W, 22);
        g.setColor(HUD_LINE);
        g.drawLine(0, 0, IsoProjection.VIEW_W, 0);
        g.drawLine(0, 21, IsoProjection.VIEW_W, 21);
        g.drawLine(0, TOP_H - 1, IsoProjection.VIEW_W, TOP_H - 1);

        drawTabs(g, scene);
        drawControls(g, speed, paused, toastsEnabled);
        drawStats(g, world);
        drawBottomBar(g, world, toastsEnabled, pacingText);
    }

    private void drawTabs(Graphics2D g, SceneId active) {
        for (SceneId scene : SceneId.values()) {
            Rectangle r = tabRects[scene.ordinal()];
            boolean isActive = scene == active;
            boolean isHover = scene == hoverTab;

            g.setColor(isActive ? TAB_ACTIVE : (isHover ? new Color(0x15, 0x1D, 0x30) : TAB_BG));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(isActive ? ACCENT : (isHover ? TEXT_SOFT : TAB_BORDER));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            if (isActive) {
                g.setColor(WARN);
                g.fillRect(r.x + 1, r.y + r.height - 3, r.width - 2, 2);
            }
            g.setFont(Font8Bit.get(8));
            g.setColor(isActive ? Color.WHITE : (isHover ? Color.WHITE : TEXT_DIM));
            String label = "[" + scene.label + "]";
            int tw = g.getFontMetrics().stringWidth(label);
            g.drawString(label, r.x + (r.width - tw) / 2, r.y + 13);
        }
    }

    private void drawControls(Graphics2D g, float speed, boolean paused, boolean toastsEnabled) {
        for (int i = 0; i < SPEEDS.length; i++) {
            Rectangle r = speedRects[i];
            boolean active = Math.abs(SPEEDS[i] - speed) < 0.001f;
            boolean hover = hoverSpeed == i;
            g.setColor(active ? WARN : (hover ? new Color(0x1D, 0x28, 0x42) : TAB_BG));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(active ? WARN : (hover ? TEXT_SOFT : TAB_BORDER));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            g.setFont(Font8Bit.get(8));
            g.setColor(active ? Color.BLACK : (hover ? Color.WHITE : TEXT_DIM));
            drawCenteredIn(g, SPEED_LABELS[i], r, 12);
        }

        drawSmallButton(g, pauseRect, paused ? "RESUME" : "PAUSE",
                paused, hoverPause, TEXT_SOFT);
        drawSmallButton(g, eventsRect, toastsEnabled ? "EVENTS:ON" : "EVENTS:OFF",
                toastsEnabled, hoverEvents, TEXT_SOFT);
    }

    private void drawSmallButton(Graphics2D g, Rectangle r, String label,
                                 boolean on, boolean highlight, Color idleText) {
        Color bg = on
                ? (highlight ? new Color(0x1C, 0x46, 0x54) : new Color(0x12, 0x2A, 0x34))
                : (highlight ? new Color(0x1D, 0x28, 0x42) : TAB_BG);
        g.setColor(bg);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(on ? ACCENT : (highlight ? TEXT_SOFT : TAB_BORDER));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setFont(Font8Bit.get(8));
        g.setColor(on ? ACCENT : (highlight ? Color.WHITE : idleText));
        drawCenteredIn(g, label, r, 12);
    }

    private void drawStats(Graphics2D g, WorldState world) {
        // Short labels: at size 8 the full names overflow the 640px row.
        int x = 6;
        x = stat(g, x, "PATS", String.valueOf(world.activePatients()), Color.WHITE);
        x = stat(g, x, "DOCS", String.valueOf(world.doctorCount), new Color(0xFF, 0xC8, 0x70));
        int waiting = world.waitingCount();
        boolean overflow = world.slots.queueOccupied() > 0;
        x = stat(g, x, "QUEUE", String.valueOf(waiting), overflow ? WARN : ACCENT);
        int beds = world.occupiedBeds();
        x = stat(g, x, "BEDS", beds + "/" + world.beds.length,
                beds == world.beds.length ? GOOD : Color.WHITE);
        x = stat(g, x, "TREATED", world.treatedCount + "/" + world.patientCount, GOOD);
        if (overflow) {
            stat(g, x, "OUT", String.valueOf(world.slots.queueOccupied()), WARN);
        }

        g.setFont(Font8Bit.get(8));
        g.setColor(TEXT_DIM);
        String speed = String.format("%.1fx", world.speed);
        int sw = g.getFontMetrics().stringWidth(speed);
        g.drawString(speed, 634 - sw, 38);
    }

    private int stat(Graphics2D g, int x, String label, String value, Color valueColor) {
        g.setFont(Font8Bit.get(8));
        FontMetrics fm = g.getFontMetrics();
        String prefix = label + ": ";
        g.setColor(TEXT_DIM);
        g.drawString(prefix, x, 38);
        int px = x + fm.stringWidth(prefix);
        g.setColor(valueColor);
        g.drawString(value, px, 38);
        return px + fm.stringWidth(value) + 8;
    }

    private void drawBottomBar(Graphics2D g, WorldState world, boolean toastsEnabled,
                               String pacingText) {
        g.setColor(HUD_BG);
        g.fillRect(0, BOTTOM_Y, IsoProjection.VIEW_W, BOTTOM_H);
        g.setColor(HUD_LINE);
        g.drawLine(0, BOTTOM_Y, IsoProjection.VIEW_W, BOTTOM_Y);

        String text;
        Color color;
        if (!toastsEnabled) {
            text = "EVENTS DISABLED  [T TO ENABLE]";
            color = TEXT_DIM;
        } else if (world.hasToast()) {
            text = "EVENT> " + world.lastToast;
            color = WARN;
        } else {
            text = "EVENT> READY";
            color = TEXT_DIM;
        }

        g.setFont(Font8Bit.get(8));
        g.setColor(color);
        String eventText = fit(g, text, 230);
        g.drawString(eventText, 6, BOTTOM_Y + 14);
        int x = 6 + g.getFontMetrics().stringWidth(eventText) + 10;

        // Pacing status sits between the event text and the key hints.
        if (pacingText != null && !pacingText.isEmpty()) {
            g.setColor(ACCENT);
            g.drawString(fit(g, pacingText, 190), x, BOTTOM_Y + 14);
            x += g.getFontMetrics().stringWidth(fit(g, pacingText, 190)) + 10;
        }

        g.setColor(TEXT_SOFT);
        String hint = "SPACE PAUSE  F FONT  V PACING  D DELAY";
        int hw = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, Math.max(x, 634 - hw), BOTTOM_Y + 14);
    }

    // ------------------------------------------------------------------
    // Overlays
    // ------------------------------------------------------------------

    public void drawOverlays(Graphics2D g, WorldState world, boolean paused) {
        if (world.complete) {
            drawCompleteOverlay(g, world);
        } else if (paused) {
            drawPauseOverlay(g);
        }
    }

    private void drawPauseOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(IsoProjection.VIEW_X, IsoProjection.VIEW_Y,
                IsoProjection.VIEW_W, IsoProjection.VIEW_H);

        int w = 280;
        int h = 82;
        int x = (IsoProjection.VIEW_W - w) / 2;
        int y = IsoProjection.VIEW_Y + 100;
        g.setColor(new Color(0x08, 0x0A, 0x12));
        g.fillRect(x, y, w, h);
        g.setColor(WARN);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(new Color(0xF8, 0xE0, 0x70));
        g.fillRect(x, y, w, 3);

        Font8Bit.drawCentered(g, "PAUSED", IsoProjection.VIEW_W / 2, y + 30, 16, Color.WHITE);
        Font8Bit.drawCentered(g, "SPACE TO RESUME", IsoProjection.VIEW_W / 2, y + 52, 8, WARN);
        Font8Bit.drawCentered(g, "SIMULATION IN BACKGROUND",
                IsoProjection.VIEW_W / 2, y + 68, 8, TEXT_DIM);
    }

    private void drawCompleteOverlay(Graphics2D g, WorldState world) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(IsoProjection.VIEW_X, IsoProjection.VIEW_Y,
                IsoProjection.VIEW_W, IsoProjection.VIEW_H);

        int w = 360;
        int h = 104;
        int x = (IsoProjection.VIEW_W - w) / 2;
        int y = IsoProjection.VIEW_Y + 94;
        g.setColor(new Color(0x06, 0x0C, 0x10));
        g.fillRect(x, y, w, h);
        g.setColor(GOOD);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(GOOD);
        g.fillRect(x, y, w, 3);

        Font8Bit.drawCentered(g, "ALL PATIENTS TREATED", IsoProjection.VIEW_W / 2,
                y + 28, 11, Color.WHITE);
        int exiting = Math.max(0, world.patientCount - world.leftCount);
        String counts = "TREATED " + world.treatedCount + "/" + world.patientCount
                + (exiting > 0 ? "    EXITING " + exiting : "    LEFT " + world.leftCount);
        Font8Bit.drawCentered(g, counts, IsoProjection.VIEW_W / 2, y + 52, 8, GOOD);
        Font8Bit.drawCentered(g, "WAITING ROOM CAPACITY " + world.capacity,
                IsoProjection.VIEW_W / 2, y + 68, 8, TEXT_DIM);

        if ((world.nowMs / 500) % 2 == 0) {
            Font8Bit.drawCentered(g, "PRESS ENTER FOR A NEW SIMULATION",
                    IsoProjection.VIEW_W / 2, y + 90, 8, WARN);
        }
    }

    // ------------------------------------------------------------------
    // Input queries / hover state
    // ------------------------------------------------------------------

    public SceneId sceneAt(int x, int y) {
        for (SceneId scene : SceneId.values()) {
            if (tabRects[scene.ordinal()].contains(x, y)) return scene;
        }
        return null;
    }

    public int speedAt(int x, int y) {
        for (int i = 0; i < speedRects.length; i++) {
            if (speedRects[i].contains(x, y)) return i;
        }
        return -1;
    }

    public boolean pauseAt(int x, int y) {
        return pauseRect.contains(x, y);
    }

    public boolean eventsAt(int x, int y) {
        return eventsRect.contains(x, y);
    }

    public boolean isInteractive(int x, int y) {
        return sceneAt(x, y) != null || speedAt(x, y) >= 0
                || pauseAt(x, y) || eventsAt(x, y);
    }

    public void setHover(int x, int y) {
        hoverTab = sceneAt(x, y);
        hoverSpeed = speedAt(x, y);
        hoverPause = pauseAt(x, y);
        hoverEvents = eventsAt(x, y);
    }

    public void clearHover() {
        hoverTab = null;
        hoverSpeed = -1;
        hoverPause = false;
        hoverEvents = false;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void drawCenteredIn(Graphics2D g, String text, Rectangle r, int baselineOffset) {
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, r.x + (r.width - tw) / 2, r.y + baselineOffset);
    }

    private int measure(String text, int size) {
        return Font8Bit.stringWidth(measureGraphics, text, size);
    }

    private static String fit(Graphics2D g, String text, int maxWidth) {
        Font font = g.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        if (fm.stringWidth(text) <= maxWidth) return text;
        String suffix = "..";
        int end = text.length();
        while (end > 0 && fm.stringWidth(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(0, end)) + suffix;
    }

    public static String speedLabel(float speed) {
        return String.format("%.1fx", speed);
    }
}

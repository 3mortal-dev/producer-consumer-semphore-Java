package gui.game;

import gui.game.iso.IsoProjection;
import gui.game.render.HudRenderer;
import gui.rendering.Font8Bit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * In-canvas start screen. No Swing widgets are used; all fields are drawn and
 * edited inside the 640x360 logical framebuffer.
 */
public final class TitleScreen {
    public record StartConfig(int capacity, int doctors, int patients, float speed,
                              Font8Bit.FontMode fontMode,
                              EventPlaybackQueue.Mode pacingMode,
                              int delayIndex) {}

    private static final int FIELD_CAPACITY = 0;
    private static final int FIELD_DOCTORS = 1;
    private static final int FIELD_PATIENTS = 2;
    private static final int FIELD_SPEED = 3;

    private static final Color BG = new Color(0x05, 0x07, 0x12);
    private static final Color FLOOR_A = new Color(0x0B, 0x11, 0x22);
    private static final Color FLOOR_B = new Color(0x0E, 0x16, 0x2A);
    private static final Color PANEL = new Color(0x08, 0x0B, 0x16, 238);
    private static final Color BORDER = new Color(0x2A, 0x3A, 0x5E);
    private static final Color WHITE = new Color(0xF8, 0xF8, 0xFF);
    private static final Color DIM = new Color(0x78, 0x84, 0xA4);
    private static final Color SOFT = new Color(0xA8, 0xB4, 0xD0);
    private static final Color ACCENT = new Color(0x58, 0xD8, 0xF0);
    private static final Color YELLOW = new Color(0xF0, 0xD8, 0x58);
    private static final Color ORANGE = new Color(0xF8, 0x80, 0x28);
    private static final Color GREEN = new Color(0x68, 0xE0, 0x80);
    private static final Color RED = new Color(0xE8, 0x38, 0x30);

    private static final int[] MIN = {1, 1, 1, 0};
    private static final int[] MAX = {60, 12, 99, HudRenderer.SPEEDS.length - 1};
    private static final String[] LABELS = {"CAPACITY", "DOCTORS", "PATIENTS", "SPEED"};

    private final int[] values = {6, 4, 12, 1};
    private final String[] edit = {"", "", "", ""};
    private final Rectangle[] fieldRects = new Rectangle[4];
    private final Rectangle startRect = new Rectangle(200, 252, 240, 24);

    // Settings chips: FONT | PACING | DELAY | TOASTS (click or F / V / D / T).
    private final Rectangle[] chipRects = {
        new Rectangle(8, 305, 152, 17),
        new Rectangle(164, 305, 152, 17),
        new Rectangle(320, 305, 152, 17),
        new Rectangle(476, 305, 156, 17),
    };
    private static final int CHIP_FONT = 0;
    private static final int CHIP_PACING = 1;
    private static final int CHIP_DELAY = 2;
    private static final int CHIP_TOASTS = 3;

    private int selected;
    private boolean startRequested;
    private boolean toastsEnabled = true;
    private Font8Bit.FontMode fontMode = Font8Bit.FontMode.PIXEL;
    private EventPlaybackQueue.Mode pacingMode = EventPlaybackQueue.Mode.DELAY;
    private int delayIndex = EventPlaybackQueue.DEFAULT_DELAY_INDEX;
    private String error = "";
    private boolean hoverStart;
    private int hoverChip = -1;
    private int hoverField = -1;

    public TitleScreen() {
        for (int i = 0; i < 4; i++) {
            fieldRects[i] = new Rectangle(330, 156 + i * 21, 146, 18);
        }
        // Start in sync with whatever typeface is currently active.
        fontMode = Font8Bit.getMode();
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    public void mousePressed(int x, int y) {
        for (int i = 0; i < fieldRects.length; i++) {
            if (fieldRects[i].contains(x, y)) {
                select(i);
                return;
            }
        }
        if (startRect.contains(x, y)) {
            requestStart();
            return;
        }
        for (int i = 0; i < chipRects.length; i++) {
            if (chipRects[i].contains(x, y)) {
                cycleChip(i);
                return;
            }
        }
    }

    public void mouseMoved(int x, int y) {
        hoverField = -1;
        for (int i = 0; i < fieldRects.length; i++) {
            if (fieldRects[i].contains(x, y)) {
                hoverField = i;
                break;
            }
        }
        hoverStart = startRect.contains(x, y);
        hoverChip = -1;
        for (int i = 0; i < chipRects.length; i++) {
            if (chipRects[i].contains(x, y)) {
                hoverChip = i;
                break;
            }
        }
    }

    private void cycleChip(int index) {
        switch (index) {
            case CHIP_FONT -> setFontMode(fontMode == Font8Bit.FontMode.PIXEL
                    ? Font8Bit.FontMode.READABLE : Font8Bit.FontMode.PIXEL);
            case CHIP_PACING -> setPacingMode(nextPacing(pacingMode));
            case CHIP_DELAY -> setDelayIndex(delayIndex + 1);
            case CHIP_TOASTS -> toastsEnabled = !toastsEnabled;
            default -> { }
        }
    }

    private static EventPlaybackQueue.Mode nextPacing(EventPlaybackQueue.Mode mode) {
        return switch (mode) {
            case OFF -> EventPlaybackQueue.Mode.DELAY;
            case DELAY -> EventPlaybackQueue.Mode.STEP;
            case STEP -> EventPlaybackQueue.Mode.OFF;
        };
    }

    public Font8Bit.FontMode fontMode() {
        return fontMode;
    }

    public void setFontMode(Font8Bit.FontMode newMode) {
        fontMode = newMode == null ? Font8Bit.FontMode.PIXEL : newMode;
        // Preview immediately: the title screen itself switches typeface.
        Font8Bit.setMode(fontMode);
    }

    public EventPlaybackQueue.Mode pacingMode() {
        return pacingMode;
    }

    public void setPacingMode(EventPlaybackQueue.Mode newMode) {
        pacingMode = newMode == null ? EventPlaybackQueue.Mode.DELAY : newMode;
    }

    public int delayIndex() {
        return delayIndex;
    }

    public void setDelayIndex(int index) {
        delayIndex = Math.floorMod(index, EventPlaybackQueue.DELAY_CHOICES_MS.length);
    }

    public String delayLabel() {
        return String.format("%.1fS",
                EventPlaybackQueue.DELAY_CHOICES_MS[delayIndex] / 1000.0);
    }

    public void keyPressed(int keyCode, char keyChar) {
        if (keyChar == 't' || keyChar == 'T') {
            toastsEnabled = !toastsEnabled;
            return;
        }
        if (keyChar == 'f' || keyChar == 'F') {
            cycleChip(CHIP_FONT);
            return;
        }
        if (keyChar == 'v' || keyChar == 'V') {
            cycleChip(CHIP_PACING);
            return;
        }
        if (keyChar == 'd' || keyChar == 'D') {
            cycleChip(CHIP_DELAY);
            return;
        }
        switch (keyCode) {
            case KeyEvent.VK_TAB -> select((selected + 1) % 4);
            case KeyEvent.VK_UP -> adjustSelected(1);
            case KeyEvent.VK_DOWN -> adjustSelected(-1);
            case KeyEvent.VK_RIGHT -> adjustSelected(1);
            case KeyEvent.VK_LEFT -> adjustSelected(-1);
            case KeyEvent.VK_ENTER -> requestStart();
            case KeyEvent.VK_BACK_SPACE -> backspace();
            default -> {
                if (keyChar >= '0' && keyChar <= '9' && selected != FIELD_SPEED) {
                    typeDigit(keyChar);
                }
            }
        }
    }

    private void select(int index) {
        selected = Math.max(0, Math.min(3, index));
        edit[selected] = "";
        error = "";
    }

    private void adjustSelected(int delta) {
        error = "";
        edit[selected] = "";
        if (selected == FIELD_SPEED) {
            int n = HudRenderer.SPEEDS.length;
            values[selected] = Math.floorMod(values[selected] + delta, n);
        } else {
            values[selected] = Math.max(MIN[selected],
                    Math.min(MAX[selected], values[selected] + delta));
        }
    }

    private void typeDigit(char c) {
        error = "";
        String current = edit[selected];
        if (current.length() >= 2) current = current.substring(1);
        current += c;
        edit[selected] = current;
        int parsed;
        try {
            parsed = Integer.parseInt(current);
        } catch (NumberFormatException ex) {
            return;
        }
        if (parsed >= MIN[selected] && parsed <= MAX[selected]) {
            values[selected] = parsed;
        }
    }

    private void backspace() {
        error = "";
        if (edit[selected].isEmpty()) return;
        edit[selected] = edit[selected].substring(0, edit[selected].length() - 1);
        if (edit[selected].isEmpty()) return;
        try {
            int parsed = Integer.parseInt(edit[selected]);
            if (parsed >= MIN[selected] && parsed <= MAX[selected]) {
                values[selected] = parsed;
            }
        } catch (NumberFormatException ignored) {
            // Keep the last valid value.
        }
    }

    private void requestStart() {
        int capacity = parseInt(edit[0], values[0]);
        int doctors = parseInt(edit[1], values[1]);
        int patients = parseInt(edit[2], values[2]);
        int speedIndex = values[3];

        if (capacity < MIN[0] || capacity > MAX[0]
                || doctors < MIN[1] || doctors > MAX[1]
                || patients < MIN[2] || patients > MAX[2]
                || speedIndex < 0 || speedIndex >= HudRenderer.SPEEDS.length) {
            error = "CHECK VALUES: CAP 1-60  DOC 1-12  PAT 1-99";
            return;
        }
        values[0] = capacity;
        values[1] = doctors;
        values[2] = patients;
        startRequested = true;
    }

    public boolean startRequested() {
        return startRequested;
    }

    public StartConfig consumeStart() {
        startRequested = false;
        return new StartConfig(values[0], values[1], values[2],
                HudRenderer.SPEEDS[values[3]], fontMode, pacingMode, delayIndex);
    }

    public boolean toastsEnabled() {
        return toastsEnabled;
    }

    public void setToastsEnabled(boolean enabled) {
        toastsEnabled = enabled;
    }

    public boolean isInteractive(int x, int y) {
        if (startRect.contains(x, y)) return true;
        for (Rectangle r : fieldRects) {
            if (r.contains(x, y)) return true;
        }
        for (Rectangle r : chipRects) {
            if (r.contains(x, y)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    public void draw(Graphics2D g, long nowMs) {
        g.setStroke(new BasicStroke(1f));
        g.setColor(BG);
        g.fillRect(0, 0, IsoProjection.VIEW_W, 360);

        drawIsoFloor(g);
        drawScanlines(g);
        drawHeader(g);

        // Title block.
        Font8Bit.drawCentered(g, "EMERGENCY", 320, 64, 26, new Color(0x10, 0x10, 0x18));
        Font8Bit.drawCentered(g, "EMERGENCY", 318, 62, 26, WHITE);
        Font8Bit.drawCentered(g, "ROOM", 320, 94, 26, new Color(0x23, 0x10, 0x08));
        Font8Bit.drawCentered(g, "ROOM", 318, 92, 26, ORANGE);
        Font8Bit.drawCentered(g, "SIMULATOR", 320, 116, 12, ACCENT);
        Font8Bit.drawCentered(g, "8-BIT ISOMETRIC EDITION", 320, 128, 8, DIM);

        drawCross(g, 92, 74, 9);
        drawCross(g, 548, 74, 9);

        drawMenuPanel(g, nowMs);
        drawFooter(g, nowMs);
        drawVignette(g);
    }

    private void drawIsoFloor(Graphics2D g) {
        // A dark isometric checkerboard fades in from the bottom of the screen.
        for (int row = 0; row < 8; row++) {
            for (int col = -10; col <= 10; col++) {
                int cx = 320 + (col - row) * 22;
                int cy = 292 + (col + row) * 11;
                if (cx < -30 || cx > 670 || cy < 220) continue;
                int[] xs = {cx, cx + 22, cx, cx - 22};
                int[] ys = {cy - 11, cy, cy + 11, cy};
                g.setColor(((row + col) & 1) == 0 ? FLOOR_A : FLOOR_B);
                g.fillPolygon(xs, ys, 4);
            }
        }
    }

    private void drawScanlines(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 26));
        for (int y = 0; y < 360; y += 3) {
            g.fillRect(0, y, IsoProjection.VIEW_W, 1);
        }
    }

    private void drawHeader(Graphics2D g) {
        g.setColor(new Color(0x0A, 0x0E, 0x1A));
        g.fillRect(0, 0, 640, 22);
        g.setColor(new Color(0x1C, 0x2A, 0x44));
        g.drawLine(0, 22, 640, 22);
        Font8Bit.drawString(g, "CS241 OPERATING SYSTEMS", 10, 14, 8, DIM);
        Font8Bit.drawRightAligned(g, "ASSIGNMENT 1  /  GROUP S7", 630, 14, 8, DIM);
        g.setColor(RED);
        g.fillRect(306, 8, 3, 8);
        g.fillRect(303, 11, 9, 3);
    }

    private void drawMenuPanel(Graphics2D g, long nowMs) {
        int x = 120;
        int y = 132;
        int w = 400;
        int h = 168;
        g.setColor(PANEL);
        g.fillRect(x, y, w, h);
        g.setColor(BORDER);
        g.drawRect(x, y, w - 1, h - 1);
        g.setColor(new Color(0x18, 0x26, 0x40));
        g.drawLine(x + 8, y + 20, x + w - 8, y + 20);

        Font8Bit.drawCentered(g, "NEW SIMULATION", 320, y + 14, 8, SOFT);

        for (int i = 0; i < 4; i++) {
            Rectangle box = fieldRects[i];
            boolean isSelected = selected == i;
            boolean isHover = hoverField == i;

            g.setFont(Font8Bit.get(8));
            g.setColor(isSelected ? YELLOW : (isHover ? WHITE : SOFT));
            g.drawString(LABELS[i], 146, box.y + 13);

            g.setColor(isSelected ? new Color(0x1A, 0x22, 0x38) : new Color(0x08, 0x0B, 0x14));
            g.fillRect(box.x, box.y, box.width, box.height);
            g.setColor(isSelected ? YELLOW : (isHover ? SOFT : BORDER));
            g.drawRect(box.x, box.y, box.width - 1, box.height - 1);

            String value = valueText(i);
            g.setFont(Font8Bit.get(8));
            int tw = g.getFontMetrics().stringWidth(value);
            g.setColor(isSelected ? Color.WHITE : SOFT);
            g.drawString(value, box.x + (box.width - tw) / 2, box.y + 13);

            if (isSelected) {
                g.setColor(YELLOW);
                if (i == FIELD_SPEED) {
                    g.drawString("<", box.x + 5, box.y + 13);
                    g.drawString(">", box.x + box.width - 12, box.y + 13);
                } else if ((nowMs / 420) % 2 == 0) {
                    g.fillRect(box.x + (box.width + tw) / 2 + 2, box.y + 4, 4, 11);
                }
            }
        }

        if (!error.isEmpty()) {
            Font8Bit.drawCentered(g, error, 320, 247, 8, RED);
        }

        boolean startHot = hoverStart || startRequested;
        g.setColor(startHot ? new Color(0x19, 0x48, 0x36) : new Color(0x0C, 0x1E, 0x1A));
        g.fillRect(startRect.x, startRect.y, startRect.width, startRect.height);
        g.setColor(startHot ? GREEN : new Color(0x3C, 0x78, 0x5C));
        g.drawRect(startRect.x, startRect.y, startRect.width - 1, startRect.height - 1);
        if ((nowMs / 500) % 2 == 0) {
            g.setColor(new Color(0x0A, 0x14, 0x10));
            g.fillRect(startRect.x + 3, startRect.y + 3, startRect.width - 6, 2);
        }
        Font8Bit.drawCentered(g, "[ START SIMULATION ]", 320, startRect.y + 16, 8,
                startHot ? Color.WHITE : GREEN);
    }

    private void drawFooter(Graphics2D g, long nowMs) {
        String[] chips = {
            "FONT: " + fontMode.name(),
            "PACING: " + pacingMode.name(),
            "DELAY: " + delayLabel(),
            toastsEnabled ? "TOASTS: ON" : "TOASTS: OFF",
        };
        for (int i = 0; i < chipRects.length; i++) {
            Rectangle r = chipRects[i];
            boolean hot = hoverChip == i;
            boolean dimmed = i == CHIP_DELAY && pacingMode != EventPlaybackQueue.Mode.DELAY;
            g.setColor(hot ? new Color(0x1D, 0x28, 0x42) : new Color(0x08, 0x0B, 0x14));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(hot ? SOFT : BORDER);
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            // Key hint letter in the top-left corner of each chip.
            g.setColor(ACCENT);
            g.setFont(Font8Bit.get(8));
            g.drawString("[" + "FVDT".charAt(i) + "]", r.x + 4, r.y + 12);
            g.setColor(dimmed ? DIM : (hot ? WHITE : SOFT));
            int tw = g.getFontMetrics().stringWidth(chips[i]);
            g.drawString(chips[i], r.x + (r.width - tw) / 2 + 8, r.y + 12);
        }

        Font8Bit.drawCentered(g,
                "TAB / CLICK SELECT   TYPE NUMBERS   ENTER START",
                320, 338, 8, DIM);
        Font8Bit.drawCentered(g, "ARROWS ADJUST   F FONT  V PACING  D DELAY  T TOASTS",
                320, 351, 8, new Color(0x55, 0x60, 0x78));
    }

    private String valueText(int index) {
        if (index == FIELD_SPEED) {
            return String.format("%.1fx", HudRenderer.SPEEDS[values[index]]);
        }
        return edit[index].isEmpty() ? String.valueOf(values[index]) : edit[index];
    }

    private static int parseInt(String text, int fallback) {
        if (text == null || text.isEmpty()) return fallback;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void drawCross(Graphics2D g, int x, int y, int arm) {
        g.setColor(new Color(0x42, 0x10, 0x14));
        g.fillRect(x - arm - 2, y - 2, arm * 2 + 4, 4);
        g.fillRect(x - 2, y - arm - 2, 4, arm * 2 + 4);
        g.setColor(RED);
        g.fillRect(x - arm, y - 1, arm * 2, 2);
        g.fillRect(x - 1, y - arm, 2, arm * 2);
        g.setColor(new Color(0xFF, 0x80, 0x78));
        g.fillRect(x - 1, y - arm, 1, arm * 2);
    }

    private void drawVignette(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRect(0, 0, 640, 3);
        g.fillRect(0, 357, 640, 3);
        g.fillRect(0, 0, 3, 360);
        g.fillRect(637, 0, 3, 360);
    }
}

package gui.components;

import gui.GuiModel;
import gui.PixelCanvas;
import gui.rendering.ColorPalette;
import gui.rendering.Font8Bit;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Scrolling event log / ticker showing the most recent simulation events.
 */
public class EventLogTicker {
    private final PixelCanvas canvas;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 52;
    private static final int PANEL_W = 992;
    private static final int PANEL_H = 44;
    private static final int LINES = 3;

    public EventLogTicker(PixelCanvas canvas) {
        this.canvas = canvas;
    }

    public void draw(Graphics2D g) {
        GuiModel model = canvas.getModel();
        if (model == null) return;

        // Panel
        g.setColor(ColorPalette.BLACK);
        g.fillRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        g.setColor(ColorPalette.BLUE_LIGHT);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(PANEL_X, PANEL_Y, PANEL_W - 1, PANEL_H - 1);
        g.setStroke(new BasicStroke(1f));

        // Header tab
        g.setColor(ColorPalette.BLUE_DARK);
        g.fillRect(PANEL_X + 2, PANEL_Y + 2, 96, 12);
        Font8Bit.drawString(g, "EVENT LOG", PANEL_X + 6, PANEL_Y + 12, 7, ColorPalette.YELLOW);

        // Last N log lines
        List<String> log = model.eventLog;
        int start = Math.max(0, log.size() - LINES);
        int textX = PANEL_X + 108;
        int textY = PANEL_Y + 14;
        for (int i = start; i < log.size(); i++) {
            String line = log.get(i);
            Font8Bit.drawString(g, line, textX, textY, 6, ColorPalette.WHITE);
            textY += 12;
        }

        if (log.isEmpty()) {
            Font8Bit.drawString(g, "awaiting simulation start...", textX, textY, 6, ColorPalette.GRAY_LIGHT);
        }
    }
}
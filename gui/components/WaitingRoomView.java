package gui.components;

import gui.GuiModel;
import gui.PixelCanvas;
import gui.rendering.ColorPalette;
import gui.rendering.Font8Bit;
import gui.rendering.SpriteAtlas;
import gui.rendering.SpriteRenderer;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Renders the waiting room area: seat grid + outside queue.
 */
public class WaitingRoomView {
    private final PixelCanvas canvas;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 104;
    private static final int PANEL_W = 992;
    private static final int PANEL_H = 184;

    private static final int OUTSIDE_X = 28;
    private static final int OUTSIDE_Y = 140;
    private static final int OUTSIDE_WIDTH = 96;

    private static final int SEAT_START_X = 150;
    private static final int SEAT_START_Y = 140;
    private static final int SEAT_SIZE = 16;
    private static final int SEAT_SPACING = 8;

    public WaitingRoomView(PixelCanvas canvas) {
        this.canvas = canvas;
    }

    public void draw(Graphics2D g) {
        GuiModel model = canvas.getModel();
        if (model == null) return;

        // Section panel
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
        g.setColor(ColorPalette.BLUE_LIGHT);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(PANEL_X, PANEL_Y, PANEL_W - 1, PANEL_H - 1);
        g.setStroke(new BasicStroke(1f));

        // Section header
        g.setColor(ColorPalette.BLACK);
        g.fillRect(PANEL_X + 2, PANEL_Y + 2, 150, 16);
        Font8Bit.drawString(g, "WAITING ROOM", PANEL_X + 8, PANEL_Y + 14, 8, ColorPalette.YELLOW);

        // Divider between outside and indoor
        g.setColor(ColorPalette.BLUE_DARK);
        g.fillRect(PANEL_X + OUTSIDE_WIDTH + 16, PANEL_Y + 20, 2, PANEL_H - 24);

        // Outside queue
        Font8Bit.drawString(g, "OUTSIDE", OUTSIDE_X, OUTSIDE_Y - 8, 7, ColorPalette.GRAY_LIGHT);
        drawOutsideQueue(g, model);

        // Indoor seats
        Font8Bit.drawString(g, "SEATS: " + occupiedSeats(model) + "/" + model.capacity,
                SEAT_START_X, PANEL_Y + 14, 7, ColorPalette.WHITE);
        drawSeatGrid(g, model);
    }

    private void drawOutsideQueue(Graphics2D g, GuiModel model) {
        if (model.outsideQueue.isEmpty()) {
            Font8Bit.drawString(g, "-none-", OUTSIDE_X, OUTSIDE_Y + 8, 6, ColorPalette.GRAY_LIGHT);
            return;
        }
        int x = OUTSIDE_X;
        int y = OUTSIDE_Y;
        int perColumn = 5; // vertical slots before wrapping
        for (int i = 0; i < model.outsideQueue.size(); i++) {
            int patientId = model.outsideQueue.get(i);
            int col = i / perColumn;
            int row = i % perColumn;
            int px = x + col * 30;
            int py = y + row * 26;

            BufferedImage sprite = SpriteAtlas.get("patient_outside");
            SpriteRenderer.draw(g, sprite, px, py);
            Font8Bit.drawString(g, "P" + patientId, px, py - 3, 6, ColorPalette.WHITE);
        }
    }

    private void drawSeatGrid(Graphics2D g, GuiModel model) {
        int capacity = model.seats.length;
        int usableWidth = (PANEL_X + PANEL_W - 12) - SEAT_START_X;
        int seatsPerRow = Math.max(1, Math.min(capacity, usableWidth / (SEAT_SIZE + SEAT_SPACING)));

        for (int i = 0; i < capacity; i++) {
            int col = i % seatsPerRow;
            int row = i / seatsPerRow;
            int x = SEAT_START_X + col * (SEAT_SIZE + SEAT_SPACING);
            int y = SEAT_START_Y + row * (SEAT_SIZE + SEAT_SPACING + 10);

            boolean occupied = model.seats[i] == GuiModel.SeatState.OCCUPIED;

            // Chair back
            BufferedImage chairSprite = occupied
                ? SpriteAtlas.get("chair_occupied")
                : SpriteAtlas.get("chair_empty");
            SpriteRenderer.draw(g, chairSprite, x, y);

            // Occupant sprite sits above chair
            if (occupied) {
                BufferedImage patientSprite = SpriteAtlas.get("patient_seated");
                SpriteRenderer.draw(g, patientSprite, x, y - 12);
            }

            // Seat number
            Font8Bit.drawString(g, String.valueOf(i + 1), x + 2, y + SEAT_SIZE + 9, 6, ColorPalette.GRAY_LIGHT);
        }
    }

    private int occupiedSeats(GuiModel model) {
        int count = 0;
        for (GuiModel.SeatState s : model.seats) {
            if (s == GuiModel.SeatState.OCCUPIED) count++;
        }
        return count;
    }
}
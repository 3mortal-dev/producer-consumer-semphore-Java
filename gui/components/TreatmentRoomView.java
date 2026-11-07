package gui.components;

import gui.GuiModel;
import gui.PixelCanvas;
import gui.rendering.ColorPalette;
import gui.rendering.Font8Bit;
import gui.rendering.SpriteAtlas;
import gui.rendering.SpriteRenderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Renders the two treatment rooms with beds, monitors, doctors, and patients.
 */
public class TreatmentRoomView {
    private final PixelCanvas canvas;

    private static final int ROOM_WIDTH = 150;
    private static final int ROOM_HEIGHT = 96;
    private static final int ROOM1_X = 24;
    private static final int ROOM_GAP = 24;
    private static final int ROOM_Y = 500;

    public TreatmentRoomView(PixelCanvas canvas) {
        this.canvas = canvas;
    }

    public void draw(Graphics2D g) {
        GuiModel model = canvas.getModel();
        if (model == null) return;

        drawRoom(g, 0, ROOM1_X, ROOM_Y, model.room1, model.room1Doctor, model.room1Patient);
        drawRoom(g, 1, ROOM1_X + ROOM_WIDTH + ROOM_GAP, ROOM_Y, model.room2, model.room2Doctor, model.room2Patient);
    }

    private void drawRoom(Graphics2D g, int roomIndex, int x, int y,
                          GuiModel.RoomState state, int doctorId, int patientId) {
        // Room background
        g.setColor(ColorPalette.BLUE_DARK);
        g.fillRect(x, y, ROOM_WIDTH, ROOM_HEIGHT);
        g.setColor(ColorPalette.GRAY_DARK);
        g.drawRect(x, y, ROOM_WIDTH - 1, ROOM_HEIGHT - 1);
        g.drawRect(x + 2, y + 2, ROOM_WIDTH - 5, ROOM_HEIGHT - 5);

        // Room label
        Font8Bit.drawString(g, "ROOM " + (roomIndex + 1), x + 6, y + 14, 8, ColorPalette.WHITE);

        // State badge (top-right)
        boolean occupied = state == GuiModel.RoomState.OCCUPIED;
        String stateText = occupied ? "OCCUPIED" : "IDLE";
        java.awt.Color stateColor = occupied ? ColorPalette.GREEN : ColorPalette.GRAY_LIGHT;
        int badgeW = Font8Bit.stringWidth(g, stateText, 7) + 8;
        g.setColor(ColorPalette.BLACK);
        g.fillRect(x + ROOM_WIDTH - badgeW - 6, y + 4, badgeW, 12);
        Font8Bit.drawString(g, stateText, x + ROOM_WIDTH - badgeW - 2, y + 14, 7, stateColor);

        // Monitor on wall
        int monitorX = x + 10;
        int monitorY = y + 22;
        BufferedImage monitorSprite = occupied
            ? SpriteAtlas.get("monitor_beating")
            : SpriteAtlas.get("monitor_idle");
        SpriteRenderer.draw(g, monitorSprite, monitorX, monitorY);

        // Bed
        int bedX = x + 40;
        int bedY = y + 40;
        BufferedImage bedSprite = occupied
            ? SpriteAtlas.get("bed_occupied")
            : SpriteAtlas.get("bed_empty");
        SpriteRenderer.draw(g, bedSprite, bedX, bedY);

        // Patient lying + doctor treating when occupied
        if (occupied) {
            BufferedImage docSprite = SpriteAtlas.get("doctor_treating", animFrame());
            SpriteRenderer.draw(g, docSprite, bedX + 20, bedY - 14);

            if (patientId > 0) {
                Font8Bit.drawString(g, "P" + patientId, bedX, bedY + 22, 7, ColorPalette.SKIN);
            }
            if (doctorId > 0) {
                Font8Bit.drawString(g, "D" + doctorId, bedX + 40, bedY - 18, 7, ColorPalette.ORANGE);
            }
        }

        // Door (right side)
        int doorX = x + ROOM_WIDTH - 18;
        int doorY = y + ROOM_HEIGHT - 24;
        BufferedImage doorSprite = occupied
            ? SpriteAtlas.get("door_open")
            : SpriteAtlas.get("door_closed");
        SpriteRenderer.draw(g, doorSprite, doorX, doorY);
    }

    /** 2-frame treatment cycle for the doctor. */
    private static int animFrame() {
        return (int) ((System.currentTimeMillis() / 250) % 2);
    }
}
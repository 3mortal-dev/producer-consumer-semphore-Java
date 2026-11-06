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
 * Bottom status bar showing each doctor's sprite and current state.
 */
public class DoctorStatusBar {
    private final PixelCanvas canvas;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 604;
    private static final int PANEL_H = 96;
    private static final int CARD_GAP = 6;

    public DoctorStatusBar(PixelCanvas canvas) {
        this.canvas = canvas;
    }

    public void draw(Graphics2D g) {
        GuiModel model = canvas.getModel();
        if (model == null) return;

        // Panel background spans full width; treatment rooms are drawn separately above.
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(PANEL_X, PANEL_Y, 992, PANEL_H);
        g.setColor(ColorPalette.BLUE_LIGHT);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(PANEL_X, PANEL_Y, 991, PANEL_H - 1);
        g.setStroke(new BasicStroke(1f));

        // Header
        g.setColor(ColorPalette.BLACK);
        g.fillRect(PANEL_X + 2, PANEL_Y + 2, 170, 16);
        Font8Bit.drawString(g, "DOCTOR STATUS", PANEL_X + 8, PANEL_Y + 14, 8, ColorPalette.YELLOW);

        int doctorCount = model.doctors.length;
        if (doctorCount == 0) return;

        int available = 992 - 16 - (doctorCount - 1) * CARD_GAP;
        int cardW = Math.min(150, available / doctorCount);
        int startX = PANEL_X + 8;
        int y = PANEL_Y + 24;
        int cardH = PANEL_H - 30;

        for (int i = 0; i < doctorCount; i++) {
            int x = startX + i * (cardW + CARD_GAP);
            drawDoctorCard(g, x, y, cardW, cardH, i + 1, model.doctors[i]);
        }
    }

    private void drawDoctorCard(Graphics2D g, int x, int y, int w, int h, int doctorId, GuiModel.DoctorState state) {
        // Card background
        g.setColor(ColorPalette.BLACK);
        g.fillRect(x, y, w, h);
        g.setColor(ColorPalette.GRAY_MED);
        g.drawRect(x, y, w - 1, h - 1);

        String label = "D" + doctorId;
        Font8Bit.drawString(g, label, x + 4, y + 12, 7, ColorPalette.WHITE);

        // Sprite (animated for idle / walking / treating)
        String spriteKey = spriteFor(state);
        BufferedImage sprite = isAnimated(spriteKey)
                ? SpriteAtlas.get(spriteKey, animFrame())
                : SpriteAtlas.get(spriteKey);
        SpriteRenderer.draw(g, sprite, x + 4, y + 18);

        // State text (wrapped/abbreviated)
        String stateText = shortState(state);
        java.awt.Color color = colorFor(state);
        Font8Bit.drawString(g, stateText, x + 24, y + 26, 6, color);
        Font8Bit.drawString(g, stateName(state), x + 24, y + 36, 5, ColorPalette.GRAY_LIGHT);
    }

    private String spriteFor(GuiModel.DoctorState state) {
        return switch (state) {
            case TREATING -> "doctor_treating";
            case TRYING_ACQUIRE_ROOM, ACQUIRED_ROOM, ASSIGNED_PATIENT -> "doctor_walking";
            case COULD_NOT_ACQUIRE, SENDING_BACK -> "doctor_sending_back";
            default -> "doctor_idle";
        };
    }

    private static boolean isAnimated(String key) {
        return key.equals("doctor_idle") || key.equals("doctor_walking") || key.equals("doctor_treating");
    }

    /** 2-frame idle/treat cycle shared across the status cards. */
    private static int animFrame() {
        return (int) ((System.currentTimeMillis() / 250) % 2);
    }

    private String shortState(GuiModel.DoctorState state) {
        return switch (state) {
            case IDLE -> "IDLE";
            case AVAILABLE -> "READY";
            case ASSIGNED_PATIENT -> "ASSIGN";
            case TRYING_ACQUIRE_ROOM -> "TRY ROOM";
            case ACQUIRED_ROOM -> "GOT ROOM";
            case TREATING -> "TREATING";
            case RELEASED_ROOM -> "RELEASED";
            case COULD_NOT_ACQUIRE -> "TIMEOUT";
            case SENDING_BACK -> "SEND BACK";
        };
    }

    private String stateName(GuiModel.DoctorState state) {
        return switch (state) {
            case IDLE -> "";
            case AVAILABLE -> "available";
            case ASSIGNED_PATIENT -> "with patient";
            case TRYING_ACQUIRE_ROOM -> "waiting...";
            case ACQUIRED_ROOM -> "entering";
            case TREATING -> "in room";
            case RELEASED_ROOM -> "cleaning";
            case COULD_NOT_ACQUIRE -> "no room!";
            case SENDING_BACK -> "returning";
        };
    }

    private java.awt.Color colorFor(GuiModel.DoctorState state) {
        return switch (state) {
            case TREATING, AVAILABLE -> ColorPalette.GREEN;
            case TRYING_ACQUIRE_ROOM, ACQUIRED_ROOM -> ColorPalette.YELLOW;
            case COULD_NOT_ACQUIRE, SENDING_BACK -> ColorPalette.RED;
            case ASSIGNED_PATIENT -> ColorPalette.BLUE_LIGHT;
            default -> ColorPalette.GRAY_LIGHT;
        };
    }
}
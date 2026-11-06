package gui.rendering;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import gui.rendering.ColorPalette;

/**
 * Programmatic 8-bit sprite atlas.
 * Generates all sprites at runtime using Java2D - zero external assets.
 * Base sprite size: 16x16 pixels (classic 8-bit tile).
 */
public final class SpriteAtlas {
    private static final int SPRITE_SIZE = 16;
    private static final Map<String, BufferedImage> sprites = new HashMap<>();

    private SpriteAtlas() {}

    /**
     * Get sprite by key. Generates on first access.
     */
    public static BufferedImage get(String key) {
        return sprites.computeIfAbsent(key, SpriteAtlas::generateSprite);
    }

    /**
     * Get sprite with specific frame for animation.
     */
    public static BufferedImage get(String baseKey, int frame) {
        return get(baseKey + "_f" + frame);
    }

    /**
     * Pre-generate all sprites (call at startup).
     */
    public static void init() {
        // Patient sprites
        get("patient_arriving"); get("patient_arriving_f1");
        get("patient_seated"); get("patient_seated_f1");
        get("patient_outside");
        get("patient_treatment"); get("patient_treatment_f1");
        get("patient_leaving"); get("patient_leaving_f1");

        // Doctor sprites
        get("doctor_idle"); get("doctor_idle_f1");
        get("doctor_walking"); get("doctor_walking_f1");
        get("doctor_acquiring");
        get("doctor_treating"); get("doctor_treating_f1");
        get("doctor_sending_back");

        // Furniture/environment
        get("chair_empty");
        get("chair_occupied");
        get("bed_empty");
        get("bed_occupied");
        get("door_closed");
        get("door_open");
        get("monitor_idle");
        get("monitor_beating");

        // UI elements
        get("heart_cross");      // Red cross for hospital
        get("arrow_up"); get("arrow_down"); get("arrow_left"); get("arrow_right");
    }

    private static BufferedImage generateSprite(String key) {
        BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Disable anti-aliasing for crisp pixels
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        drawSprite(g, key);

        g.dispose();
        return img;
    }

    private static void drawSprite(Graphics2D g, String key) {
        switch (key) {
            // ========== PATIENT SPRITES ==========
            case "patient_arriving":
                drawPatient(g, 1, 0, false); // frame 0: left foot forward
                break;
            case "patient_arriving_f1":
                drawPatient(g, 1, 0, true);  // frame 1: right foot forward
                break;
            case "patient_seated":
                drawPatientSeated(g, false);
                break;
            case "patient_seated_f1":
                drawPatientSeated(g, true);  // slight head bob
                break;
            case "patient_outside":
                drawPatientStanding(g);
                break;
            case "patient_treatment":
                drawPatientLying(g, false);
                break;
            case "patient_treatment_f1":
                drawPatientLying(g, true);   // chest rise
                break;
            case "patient_leaving":
                drawPatient(g, -1, 0, false); // walking right
                break;
            case "patient_leaving_f1":
                drawPatient(g, -1, 0, true);

            // ========== DOCTOR SPRITES ==========
            case "doctor_idle":
                drawDoctor(g, 0, false);
                break;
            case "doctor_idle_f1":
                drawDoctor(g, 0, true);      // breathing
                break;
            case "doctor_walking":
                drawDoctor(g, 1, false);
                break;
            case "doctor_walking_f1":
                drawDoctor(g, 1, true);
                break;
            case "doctor_acquiring":
                drawDoctor(g, 2, false);     // hand on door
                break;
            case "doctor_treating":
                drawDoctorTreating(g, false);
                break;
            case "doctor_treating_f1":
                drawDoctorTreating(g, true); // stethoscope bounce
                break;
            case "doctor_sending_back":
                drawDoctorSendingBack(g);
                break;

            // ========== FURNITURE ==========
            case "chair_empty":
                drawChair(g, false);
                break;
            case "chair_occupied":
                drawChair(g, true);
                break;
            case "bed_empty":
                drawBed(g, false);
                break;
            case "bed_occupied":
                drawBed(g, true);
                break;
            case "door_closed":
                drawDoor(g, false);
                break;
            case "door_open":
                drawDoor(g, true);
                break;
            case "monitor_idle":
                drawMonitor(g, false);
                break;
            case "monitor_beating":
                drawMonitor(g, true);
                break;

            // ========== UI ICONS ==========
            case "heart_cross":
                drawHeartCross(g);
                break;
            case "arrow_up":
                drawArrow(g, 0);
                break;
            case "arrow_down":
                drawArrow(g, 1);
                break;
            case "arrow_left":
                drawArrow(g, 2);
                break;
            case "arrow_right":
                drawArrow(g, 3);
                break;

            default:
                // Missing sprite - magenta placeholder
                g.setColor(new java.awt.Color(0xFF, 0x00, 0xFF));
                g.fillRect(0, 0, SPRITE_SIZE, SPRITE_SIZE);
                g.setColor(ColorPalette.WHITE);
                g.drawString("?", 4, 12);
        }
    }

    // ========== PATIENT DRAWING ==========
    private static void drawPatient(Graphics2D g, int dir, int offset, boolean frame2) {
        // 16x16 sprite, patient facing down (toward viewer), walking animation
        // Head
        g.setColor(ColorPalette.SKIN);
        g.fillRect(5, 1, 6, 5);
        // Eyes
        g.setColor(ColorPalette.BLACK);
        g.fillRect(6, 2, 1, 1);
        g.fillRect(9, 2, 1, 1);
        // Gown
        g.setColor(ColorPalette.PATIENT_GOWN);
        g.fillRect(3, 6, 10, 8);
        // Red cross on gown
        g.setColor(ColorPalette.RED);
        g.fillRect(7, 7, 2, 2);
        g.fillRect(6, 8, 4, 1);
        // Legs (walking animation)
        g.setColor(ColorPalette.SKIN);
        if (frame2) {
            g.fillRect(5, 14, 3, 2);  // left leg forward
            g.fillRect(9, 14, 2, 2);  // right leg back
        } else {
            g.fillRect(5, 14, 2, 2);
            g.fillRect(9, 14, 3, 2);
        }
        // Feet
        g.setColor(ColorPalette.GRAY_DARK);
        if (frame2) {
            g.fillRect(5, 16, 3, 1);
            g.fillRect(9, 16, 2, 1);
        } else {
            g.fillRect(5, 16, 2, 1);
            g.fillRect(9, 16, 3, 1);
        }
    }

    private static void drawPatientSeated(Graphics2D g, boolean frame2) {
        // Patient sitting in chair (only head/shoulders visible above chair back)
        g.setColor(ColorPalette.SKIN);
        g.fillRect(5, 1, 6, 5);  // head
        g.setColor(ColorPalette.BLACK);
        g.fillRect(6, 2, 1, 1);
        g.fillRect(9, 2, 1, 1);
        // Shoulders/gown visible
        g.setColor(ColorPalette.PATIENT_GOWN);
        g.fillRect(4, 6, 8, 4);
        g.setColor(ColorPalette.RED);
        g.fillRect(7, 7, 2, 1);
        // Slight head bob
        if (frame2) {
            g.translate(0, 1);
        }
    }

    private static void drawPatientStanding(Graphics2D g) {
        // Standing patient (waiting outside)
        drawPatient(g, 1, 0, false);
    }

    private static void drawPatientLying(Graphics2D g, boolean frame2) {
        // Patient lying on bed (horizontal, head left)
        g.setColor(ColorPalette.PATIENT_GOWN);
        g.fillRect(1, 5, 14, 7); // body
        g.setColor(ColorPalette.RED);
        g.fillRect(6, 6, 4, 2);  // cross
        // Head
        g.setColor(ColorPalette.SKIN);
        g.fillRect(1, 3, 6, 5);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(2, 4, 1, 1);
        g.fillRect(5, 4, 1, 1);
        // Chest rise animation
        if (frame2) {
            g.setColor(ColorPalette.PATIENT_GOWN);
            g.fillRect(1, 5, 14, 8); // slightly taller
        }
    }

    // ========== DOCTOR DRAWING ==========
    private static void drawDoctor(Graphics2D g, int state, boolean frame2) {
        // state: 0=idle, 1=walking, 2=acquiring (at door)
        // Head
        g.setColor(ColorPalette.SKIN);
        g.fillRect(5, 1, 6, 5);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(6, 2, 1, 1);
        g.fillRect(9, 2, 1, 1);
        // Hair
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(5, 0, 6, 2);
        // Coat
        g.setColor(ColorPalette.DOCTOR_COAT);
        g.fillRect(3, 6, 10, 8);
        // White collar
        g.setColor(ColorPalette.WHITE);
        g.fillRect(5, 6, 6, 1);
        // Stethoscope
        g.setColor(ColorPalette.BLACK);
        g.drawLine(7, 7, 5, 10);
        g.drawLine(10, 7, 12, 10);
        g.fillOval(4, 10, 3, 2);
        g.fillOval(11, 10, 3, 2);
        // Legs
        g.setColor(ColorPalette.GRAY_DARK);
        if (state == 1) { // walking
            if (frame2) {
                g.fillRect(5, 14, 3, 2);
                g.fillRect(9, 14, 2, 2);
            } else {
                g.fillRect(5, 14, 2, 2);
                g.fillRect(9, 14, 3, 2);
            }
        } else {
            g.fillRect(5, 14, 3, 2);
            g.fillRect(9, 14, 3, 2);
        }
        // Feet
        g.setColor(ColorPalette.BLACK);
        if (state == 1 && frame2) {
            g.fillRect(5, 16, 3, 1);
            g.fillRect(9, 16, 2, 1);
        } else {
            g.fillRect(5, 16, 3, 1);
            g.fillRect(9, 16, 3, 1);
        }
        // Acquiring state - hand reaching
        if (state == 2) {
            g.setColor(ColorPalette.SKIN);
            g.fillRect(13, 8, 3, 4); // right arm extended
        }
    }

    private static void drawDoctorTreating(Graphics2D g, boolean frame2) {
        // Doctor at bedside, leaning over patient
        g.setColor(ColorPalette.SKIN);
        g.fillRect(5, 1, 6, 5);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(6, 2, 1, 1);
        g.fillRect(9, 2, 1, 1);
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(5, 0, 6, 2);
        // Coat
        g.setColor(ColorPalette.DOCTOR_COAT);
        g.fillRect(2, 6, 12, 8);
        g.setColor(ColorPalette.WHITE);
        g.fillRect(5, 6, 6, 1);
        // Stethoscope - animated
        g.setColor(ColorPalette.BLACK);
        int bounce = frame2 ? 1 : 0;
        g.drawLine(7, 7 + bounce, 4, 11 + bounce);
        g.drawLine(10, 7 + bounce, 13, 11 + bounce);
        g.fillOval(3, 11 + bounce, 3, 2);
        g.fillOval(12, 11 + bounce, 3, 2);
    }

    private static void drawDoctorSendingBack(Graphics2D g) {
        // Doctor gesturing "go back"
        drawDoctor(g, 0, false);
        // Right arm pointing left
        g.setColor(ColorPalette.SKIN);
        g.fillRect(0, 8, 4, 8);
        g.setColor(ColorPalette.DOCTOR_COAT);
        g.fillRect(0, 6, 4, 2);
    }

    // ========== FURNITURE ==========
    private static void drawChair(Graphics2D g, boolean occupied) {
        // 16x16 chair viewed from front
        g.setColor(ColorPalette.BROWN);
        // Seat
        g.fillRect(2, 10, 12, 4);
        // Back
        g.fillRect(2, 2, 12, 8);
        // Legs
        g.fillRect(3, 14, 2, 2);
        g.fillRect(11, 14, 2, 2);
        if (occupied) {
            // Patient head visible above chair back
            g.setColor(ColorPalette.SKIN);
            g.fillRect(5, 0, 6, 4);
            g.setColor(ColorPalette.BLACK);
            g.fillRect(6, 1, 1, 1);
            g.fillRect(9, 1, 1, 1);
        }
    }

    private static void drawBed(Graphics2D g, boolean occupied) {
        // Hospital bed, side view
        g.setColor(ColorPalette.BED_FRAME);
        // Frame
        g.fillRect(0, 8, 16, 8);  // mattress base
        g.setColor(ColorPalette.METAL);
        g.fillRect(0, 7, 16, 1);  // top rail
        g.fillRect(0, 16, 16, 1); // bottom
        // Headboard
        g.fillRect(0, 7, 2, 10);
        g.fillRect(14, 7, 2, 10);
        // Mattress
        g.setColor(ColorPalette.BED_SHEET);
        g.fillRect(1, 9, 14, 6);
        // Pillow
        g.fillRect(1, 9, 6, 3);
        if (occupied) {
            // Patient lying - head on pillow
            g.setColor(ColorPalette.SKIN);
            g.fillRect(1, 5, 6, 5);
            g.setColor(ColorPalette.BLACK);
            g.fillRect(2, 6, 1, 1);
            g.fillRect(5, 6, 1, 1);
            g.setColor(ColorPalette.PATIENT_GOWN);
            g.fillRect(1, 10, 14, 5);
            g.setColor(ColorPalette.RED);
            g.fillRect(6, 11, 4, 1);
        }
    }

    private static void drawDoor(Graphics2D g, boolean open) {
        // Door frame
        g.setColor(ColorPalette.GRAY_DARK);
        g.drawRect(1, 1, 14, 14);
        if (open) {
            // Door swung open (visible as thin line)
            g.setColor(ColorPalette.BROWN);
            g.fillRect(14, 1, 2, 14);
        } else {
            // Door closed
            g.setColor(ColorPalette.BROWN);
            g.fillRect(2, 2, 12, 12);
            // Window
            g.setColor(ColorPalette.BLUE_LIGHT);
            g.fillRect(5, 4, 6, 4);
            g.setColor(ColorPalette.GRAY_DARK);
            g.drawLine(8, 4, 8, 7);
            g.drawLine(5, 5, 10, 5);
            // Handle
            g.setColor(ColorPalette.METAL);
            g.fillRect(11, 8, 2, 1);
        }
    }

    private static void drawMonitor(Graphics2D g, boolean beating) {
        // Heart monitor
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(1, 1, 14, 10); // screen
        g.setColor(ColorPalette.MONITOR_BG);
        g.fillRect(2, 2, 12, 8);
        if (beating) {
            g.setColor(ColorPalette.MONITOR_LINE);
            // Heartbeat line
            int[] x = {2, 4, 5, 6, 7, 8, 9, 10, 11, 13};
            int[] y = {6, 6, 4, 6, 8, 6, 4, 6, 8, 6};
            for (int i = 0; i < x.length - 1; i++) {
                g.drawLine(x[i], y[i], x[i+1], y[i+1]);
            }
        } else {
            // Flat line
            g.setColor(ColorPalette.RED);
            g.drawLine(2, 6, 13, 6);
        }
        // Base
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(5, 11, 6, 4);
    }

    // ========== UI ICONS ==========
    private static void drawHeartCross(Graphics2D g) {
        g.setColor(ColorPalette.RED);
        g.fillRect(7, 3, 2, 10);
        g.fillRect(3, 7, 10, 2);
    }

    private static void drawArrow(Graphics2D g, int dir) {
        // 0=up, 1=down, 2=left, 3=right
        g.setColor(ColorPalette.WHITE);
        int cx = 8, cy = 8;
        switch (dir) {
            case 0: // up
                g.fillPolygon(new int[]{cx, cx-3, cx+3}, new int[]{cy-3, cy+2, cy+2}, 3);
                break;
            case 1: // down
                g.fillPolygon(new int[]{cx, cx-3, cx+3}, new int[]{cy+3, cy-2, cy-2}, 3);
                break;
            case 2: // left
                g.fillPolygon(new int[]{cx-3, cx+2, cx+2}, new int[]{cy, cy-3, cy+3}, 3);
                break;
            case 3: // right
                g.fillPolygon(new int[]{cx+3, cx-2, cx-2}, new int[]{cy, cy-3, cy+3}, 3);
                break;
        }
    }

    public static int getSpriteSize() {
        return SPRITE_SIZE;
    }
}
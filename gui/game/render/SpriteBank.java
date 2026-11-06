package gui.game.render;

import gui.game.world.ActorKind;
import gui.game.world.Facing;
import gui.rendering.ColorPalette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Procedural 8-bit sprite factory. Every character and furniture sprite is
 * drawn with Graphics2D primitives and cached; there are no external images.
 */
public final class SpriteBank {
    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private static final Color OUTLINE = new Color(0x18, 0x18, 0x24);
    private static final Color HAIR = new Color(0x58, 0x30, 0x18);
    private static final Color HAIR_GRAY = new Color(0x9A, 0x9A, 0xA8);
    private static final Color SCRUB = new Color(0xE8, 0xF0, 0xF0);
    private static final Color PANTS = new Color(0x30, 0x48, 0x78);
    private static final Color CHAIR_RED = new Color(0xD8, 0x20, 0x10);
    private static final Color CHAIR_ORANGE = new Color(0xF8, 0x78, 0x00);
    private static final Color CHAIR_DARK = new Color(0xA8, 0x40, 0x00);
    private static final Color BED_FRAME = new Color(0xB8, 0xB8, 0xB8);
    private static final Color BED_DARK = new Color(0x58, 0x58, 0x58);
    private static final Color SHEET = new Color(0xE8, 0xF8, 0xFF);
    private static final Color SCREEN_DARK = new Color(0x08, 0x20, 0x20);
    private static final Color SCREEN_GREEN = new Color(0x48, 0xFF, 0x90);
    private static final Color GLASS = new Color(0x88, 0xD8, 0xF8, 150);

    private SpriteBank() {}

    /** Pre-generates the common sprite set on the EDT. */
    public static void init() {
        for (ActorKind kind : ActorKind.values()) {
            for (Facing facing : Facing.values()) {
                for (Pose pose : new Pose[]{Pose.STAND, Pose.WALK, Pose.SIT, Pose.TREAT, Pose.GESTURE}) {
                    character(kind, pose, facing, 0);
                    character(kind, pose, facing, 1);
                }
            }
            lying(kind);
        }
        chair();
        bed();
        ivDrip();
        monitor();
        reception();
        plant();
        stanchion();
        waterCooler();
        vendingMachine();
    }

    public static BufferedImage character(ActorKind kind, Pose pose, Facing facing, int frame) {
        String key = "char:" + kind + ":" + pose + ":" + facing + ":" + (frame & 1);
        return CACHE.computeIfAbsent(key, k -> generateCharacter(kind, pose, facing, frame & 1));
    }

    public static BufferedImage lying(ActorKind kind) {
        return CACHE.computeIfAbsent("lie:" + kind, k -> generateLying(kind));
    }

    public static BufferedImage chair() {
        return CACHE.computeIfAbsent("chair", k -> generateChair());
    }

    public static BufferedImage bed() {
        return CACHE.computeIfAbsent("bed", k -> generateBed());
    }

    public static BufferedImage ivDrip() {
        return CACHE.computeIfAbsent("iv", k -> generateIv());
    }

    public static BufferedImage monitor() {
        return CACHE.computeIfAbsent("monitor", k -> generateMonitor());
    }

    public static BufferedImage reception() {
        return CACHE.computeIfAbsent("reception", k -> generateReception());
    }

    public static BufferedImage plant() {
        return CACHE.computeIfAbsent("plant", k -> generatePlant());
    }

    public static BufferedImage stanchion() {
        return CACHE.computeIfAbsent("stanchion", k -> generateStanchion());
    }

    public static BufferedImage waterCooler() {
        return CACHE.computeIfAbsent("cooler", k -> generateCooler());
    }

    public static BufferedImage vendingMachine() {
        return CACHE.computeIfAbsent("vending", k -> generateVendingMachine());
    }

    // ------------------------------------------------------------------
    // Characters
    // ------------------------------------------------------------------

    private static BufferedImage generateCharacter(ActorKind kind, Pose pose, Facing facing, int frame) {
        boolean back = facing == Facing.NE || facing == Facing.NW;
        boolean mirror = facing == Facing.SW || facing == Facing.NW;
        boolean doctor = kind == ActorKind.DOCTOR;

        BufferedImage img = new BufferedImage(28, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        int shiftX = pose == Pose.TREAT ? 2 : 0;
        boolean walking = pose == Pose.WALK;
        boolean sitting = pose == Pose.SIT || pose == Pose.RECOVER;

        if (sitting) {
            drawSitting(g, doctor, back, frame);
        } else {
            drawStanding(g, doctor, back, walking, frame, shiftX);
        }

        if (pose == Pose.TREAT) drawTreatArm(g);
        if (pose == Pose.GESTURE) drawGestureArm(g);

        g.dispose();
        return mirror ? mirror(img) : img;
    }

    private static void drawStanding(Graphics2D g, boolean doctor, boolean back,
                                     boolean walking, int frame, int shiftX) {
        int x = 7 + shiftX;
        Color body = doctor ? ColorPalette.ORANGE : ColorPalette.OFF_WHITE;
        Color pants = doctor ? PANTS : new Color(0xC8, 0xD8, 0xD8);

        // Shadow is drawn by the renderer, not baked in.
        // Head silhouette and skin.
        g.setColor(OUTLINE);
        g.fillRect(x + 1, 3, 12, 12);
        g.setColor(ColorPalette.SKIN);
        g.fillRect(x + 3, 5, 8, 8);

        // Hair cap / back of head.
        g.setColor(doctor ? HAIR_GRAY : HAIR);
        g.fillRect(x + 2, 3, 10, 4);
        if (back) {
            g.fillRect(x + 2, 5, 10, 6);
            g.setColor(doctor ? HAIR_GRAY : HAIR);
            g.fillRect(x + 3, 6, 8, 5);
        } else {
            g.fillRect(x + 2, 4, 2, 5);
            g.fillRect(x + 12, 4, 2, 5);
            // Eyes
            g.setColor(ColorPalette.BLACK);
            g.fillRect(x + 5, 9, 2, 2);
            g.fillRect(x + 10, 9, 2, 2);
        }

        // Torso.
        g.setColor(OUTLINE);
        g.fillRect(x + 0, 15, 14, 13);
        g.setColor(body);
        g.fillRect(x + 2, 16, 10, 11);

        if (doctor) {
            g.setColor(ColorPalette.WHITE);
            g.fillRect(x + 4, 16, 6, 2);
            g.setColor(ColorPalette.BLACK);
            g.drawLine(x + 6, 18, x + 9, 22);
            g.fillOval(x + 8, 21, 3, 2);
            g.setColor(ColorPalette.WHITE);
            g.fillRect(x + 3, 24, 9, 2);
        } else {
            g.setColor(ColorPalette.RED);
            g.fillRect(x + 5, 19, 3, 5);
            g.fillRect(x + 4, 20, 5, 3);
            g.setColor(new Color(0xD8, 0xE8, 0xE8));
            g.fillRect(x + 2, 24, 10, 2);
        }

        // Arms.
        g.setColor(OUTLINE);
        g.fillRect(x - 2, 16, 4, 10);
        g.fillRect(x + 12, 16, 4, 10);
        g.setColor(ColorPalette.SKIN);
        g.fillRect(x - 1, 17, 2, 7);
        g.fillRect(x + 13, 17, 2, 7);

        // Legs, alternating with walk frame.
        int leftOffset = walking ? (frame == 0 ? -1 : 1) : 0;
        int rightOffset = walking ? (frame == 0 ? 1 : -1) : 0;
        g.setColor(OUTLINE);
        g.fillRect(x + 1 + leftOffset, 27, 5, 9);
        g.fillRect(x + 8 + rightOffset, 27, 5, 9);
        g.setColor(pants);
        g.fillRect(x + 2 + leftOffset, 28, 3, 6);
        g.fillRect(x + 9 + rightOffset, 28, 3, 6);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(x + 1 + leftOffset, 33, 5, 2);
        g.fillRect(x + 8 + rightOffset, 33, 5, 2);
    }

    private static void drawSitting(Graphics2D g, boolean doctor, boolean back, int frame) {
        int x = 8;
        Color body = doctor ? ColorPalette.ORANGE : ColorPalette.OFF_WHITE;
        Color legs = doctor ? PANTS : new Color(0xC8, 0xD8, 0xD8);

        g.setColor(OUTLINE);
        g.fillRect(x + 1, 1, 12, 12);
        g.setColor(ColorPalette.SKIN);
        g.fillRect(x + 3, 3, 8, 8);
        g.setColor(doctor ? HAIR_GRAY : HAIR);
        g.fillRect(x + 2, 1, 10, 3);
        if (back) {
            g.fillRect(x + 2, 3, 10, 6);
        } else {
            g.setColor(ColorPalette.BLACK);
            g.fillRect(x + 5, 6, 2, 2);
            g.fillRect(x + 10, 6, 2, 2);
        }

        g.setColor(OUTLINE);
        g.fillRect(x, 12, 14, 10);
        g.setColor(body);
        g.fillRect(x + 2, 13, 10, 8);
        if (doctor) {
            g.setColor(ColorPalette.WHITE);
            g.fillRect(x + 4, 13, 6, 2);
        } else {
            g.setColor(ColorPalette.RED);
            g.fillRect(x + 6, 15, 3, 4);
        }

        // Bent legs and shoes.
        int kick = frame == 0 ? 0 : 1;
        g.setColor(OUTLINE);
        g.fillRect(x + 1, 21, 5, 5);
        g.fillRect(x + 8 + kick, 21, 5, 5);
        g.setColor(legs);
        g.fillRect(x + 2, 22, 3, 3);
        g.fillRect(x + 9 + kick, 22, 3, 3);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(x + 1, 25, 5, 2);
        g.fillRect(x + 8 + kick, 25, 5, 2);
    }

    private static void drawTreatArm(Graphics2D g) {
        g.setColor(OUTLINE);
        g.fillRect(17, 17, 10, 5);
        g.setColor(ColorPalette.SKIN);
        g.fillRect(18, 18, 8, 3);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(25, 18, 2, 2);
    }

    private static void drawGestureArm(Graphics2D g) {
        g.setColor(OUTLINE);
        g.fillRect(0, 17, 9, 5);
        g.setColor(ColorPalette.SKIN);
        g.fillRect(1, 18, 7, 3);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(1, 18, 2, 2);
    }

    private static BufferedImage generateLying(ActorKind kind) {
        boolean doctor = kind == ActorKind.DOCTOR;
        BufferedImage img = new BufferedImage(48, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);
        Color body = doctor ? ColorPalette.ORANGE : ColorPalette.OFF_WHITE;

        // Legs/feet at the right.
        g.setColor(OUTLINE);
        g.fillRect(28, 8, 18, 11);
        g.setColor(doctor ? PANTS : new Color(0xC8, 0xD8, 0xD8));
        g.fillRect(30, 10, 14, 7);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(42, 9, 4, 8);

        // Torso.
        g.setColor(OUTLINE);
        g.fillRect(14, 5, 20, 15);
        g.setColor(body);
        g.fillRect(16, 7, 16, 11);
        if (!doctor) {
            g.setColor(ColorPalette.RED);
            g.fillRect(23, 10, 3, 5);
            g.fillRect(22, 11, 5, 3);
        } else {
            g.setColor(ColorPalette.WHITE);
            g.fillRect(17, 7, 11, 2);
        }

        // Head and pillow.
        g.setColor(ColorPalette.WHITE);
        g.fillRect(2, 3, 15, 16);
        g.setColor(OUTLINE);
        g.fillRect(4, 4, 12, 13);
        g.setColor(ColorPalette.SKIN);
        g.fillRect(6, 6, 9, 9);
        g.setColor(doctor ? HAIR_GRAY : HAIR);
        g.fillRect(6, 4, 9, 3);
        if (!doctor) {
            g.setColor(ColorPalette.BLACK);
            g.fillRect(9, 9, 2, 1);
        }

        // Arm resting on the blanket.
        g.setColor(ColorPalette.SKIN);
        g.fillRect(20, 16, 9, 3);
        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------
    // Furniture
    // ------------------------------------------------------------------

    private static BufferedImage generateChair() {
        BufferedImage img = new BufferedImage(32, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // Backrest.
        g.setColor(OUTLINE);
        g.fillRect(4, 5, 15, 17);
        g.setColor(CHAIR_RED);
        g.fillRect(6, 7, 11, 13);
        g.setColor(new Color(0xA8, 0x18, 0x08));
        g.fillRect(8, 9, 7, 3);

        // Seat diamond.
        g.setColor(OUTLINE);
        g.fillPolygon(new int[]{16, 29, 16, 3}, new int[]{17, 23, 29, 23}, 4);
        g.setColor(CHAIR_ORANGE);
        g.fillPolygon(new int[]{16, 27, 16, 5}, new int[]{19, 23, 27, 23}, 4);
        g.setColor(CHAIR_DARK);
        g.fillRect(14, 23, 5, 2);

        // Legs.
        g.setColor(OUTLINE);
        g.fillRect(7, 26, 4, 8);
        g.fillRect(21, 26, 4, 8);
        g.setColor(BED_FRAME);
        g.fillRect(8, 27, 2, 5);
        g.fillRect(22, 27, 2, 5);

        g.dispose();
        return img;
    }

    private static BufferedImage generateBed() {
        BufferedImage img = new BufferedImage(76, 56, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // Frame / shadow base.
        g.setColor(OUTLINE);
        g.fillPolygon(new int[]{38, 73, 38, 3}, new int[]{18, 32, 46, 32}, 4);
        g.setColor(BED_DARK);
        g.fillPolygon(new int[]{38, 70, 38, 6}, new int[]{20, 31, 43, 31}, 4);

        // Mattress.
        g.setColor(BED_FRAME);
        g.fillPolygon(new int[]{38, 69, 38, 7}, new int[]{13, 27, 40, 27}, 4);
        g.setColor(SHEET);
        g.fillPolygon(new int[]{38, 66, 38, 10}, new int[]{15, 27, 37, 27}, 4);

        // Pillow.
        g.setColor(ColorPalette.WHITE);
        g.fillPolygon(new int[]{14, 25, 14, 7}, new int[]{19, 24, 30, 25}, 4);
        g.setColor(new Color(0xD0, 0xE8, 0xF0));
        g.fillRect(12, 24, 8, 2);

        // Blanket fold.
        g.setColor(new Color(0xB8, 0xD8, 0xE8));
        g.fillRect(30, 25, 28, 2);

        // Wheels / legs.
        g.setColor(OUTLINE);
        g.fillRect(12, 38, 4, 9);
        g.fillRect(58, 38, 4, 9);
        g.setColor(BED_FRAME);
        g.fillRect(13, 39, 2, 6);
        g.fillRect(59, 39, 2, 6);

        g.dispose();
        return img;
    }

    private static BufferedImage generateIv() {
        BufferedImage img = new BufferedImage(26, 56, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // Base and pole.
        g.setColor(OUTLINE);
        g.fillRect(10, 4, 4, 46);
        g.fillRect(4, 48, 16, 3);
        g.setColor(BED_FRAME);
        g.fillRect(11, 5, 2, 43);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(3, 51, 4, 3);
        g.fillRect(18, 51, 4, 3);

        // Bag.
        g.setColor(OUTLINE);
        g.fillRect(4, 2, 12, 16);
        g.setColor(GLASS);
        g.fillRect(6, 4, 8, 12);
        g.setColor(new Color(0x70, 0xC8, 0xE0));
        g.fillRect(7, 9, 6, 6);

        // Tube.
        g.setColor(new Color(0x60, 0xB0, 0xC0));
        g.drawLine(14, 14, 20, 20);
        g.drawLine(20, 20, 18, 32);
        g.dispose();
        return img;
    }

    private static BufferedImage generateMonitor() {
        BufferedImage img = new BufferedImage(32, 46, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // Cart.
        g.setColor(OUTLINE);
        g.fillRect(6, 26, 20, 18);
        g.setColor(BED_DARK);
        g.fillRect(8, 28, 16, 12);
        g.setColor(BED_FRAME);
        g.fillRect(10, 40, 3, 3);
        g.fillRect(20, 40, 3, 3);

        // Screen housing.
        g.setColor(OUTLINE);
        g.fillRect(3, 3, 26, 22);
        g.setColor(new Color(0x30, 0x38, 0x40));
        g.fillRect(5, 5, 22, 18);
        g.setColor(SCREEN_DARK);
        g.fillRect(7, 7, 18, 13);

        // Static ECG trace.
        g.setColor(SCREEN_GREEN);
        g.drawLine(8, 14, 11, 14);
        g.drawLine(11, 14, 12, 10);
        g.drawLine(12, 10, 13, 18);
        g.drawLine(13, 18, 14, 12);
        g.drawLine(14, 12, 16, 14);
        g.drawLine(16, 14, 23, 14);
        g.setColor(new Color(0x20, 0x80, 0x40));
        g.drawRect(7, 7, 18, 13);

        g.dispose();
        return img;
    }

    private static BufferedImage generateReception() {
        BufferedImage img = new BufferedImage(88, 56, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        g.setColor(OUTLINE);
        g.fillPolygon(new int[]{44, 85, 44, 3}, new int[]{16, 32, 48, 32}, 4);
        g.setColor(new Color(0x70, 0x50, 0x30));
        g.fillPolygon(new int[]{44, 82, 44, 6}, new int[]{18, 31, 45, 31}, 4);

        // Counter top.
        g.setColor(new Color(0xC8, 0xA8, 0x78));
        g.fillPolygon(new int[]{44, 79, 44, 9}, new int[]{17, 29, 41, 29}, 4);
        g.setColor(new Color(0xE8, 0xD0, 0xA0));
        g.fillPolygon(new int[]{44, 74, 44, 14}, new int[]{19, 29, 37, 29}, 4);

        // Monitor + papers.
        g.setColor(OUTLINE);
        g.fillRect(40, 8, 12, 10);
        g.setColor(SCREEN_GREEN);
        g.fillRect(42, 10, 8, 6);
        g.setColor(ColorPalette.WHITE);
        g.fillRect(20, 23, 8, 4);
        g.fillRect(50, 24, 7, 3);

        g.dispose();
        return img;
    }

    private static BufferedImage generatePlant() {
        BufferedImage img = new BufferedImage(26, 34, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // Pot.
        g.setColor(OUTLINE);
        g.fillRect(6, 23, 14, 9);
        g.setColor(new Color(0xA8, 0x50, 0x00));
        g.fillRect(7, 24, 12, 7);

        // Leaves.
        g.setColor(new Color(0x18, 0x58, 0x18));
        g.fillRect(11, 4, 4, 19);
        g.fillRect(5, 10, 4, 11);
        g.fillRect(17, 8, 4, 13);
        g.setColor(ColorPalette.GREEN);
        g.fillRect(12, 5, 2, 14);
        g.fillRect(6, 11, 2, 8);
        g.fillRect(18, 9, 2, 10);
        g.dispose();
        return img;
    }

    private static BufferedImage generateStanchion() {
        BufferedImage img = new BufferedImage(18, 38, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        g.setColor(OUTLINE);
        g.fillRect(7, 7, 4, 25);
        g.setColor(BED_FRAME);
        g.fillRect(8, 8, 2, 23);
        g.setColor(OUTLINE);
        g.fillOval(4, 2, 10, 8);
        g.setColor(CHAIR_RED);
        g.fillOval(5, 3, 8, 6);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(2, 32, 14, 3);
        g.dispose();
        return img;
    }

    private static BufferedImage generateCooler() {
        BufferedImage img = new BufferedImage(26, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        g.setColor(OUTLINE);
        g.fillRect(5, 12, 16, 26);
        g.setColor(new Color(0xC8, 0xC8, 0xD8));
        g.fillRect(7, 14, 12, 22);
        g.setColor(new Color(0x58, 0xA8, 0xE8, 180));
        g.fillRect(8, 4, 10, 10);
        g.setColor(ColorPalette.BLACK);
        g.fillRect(10, 22, 6, 2);
        g.fillRect(10, 27, 6, 2);
        g.dispose();
        return img;
    }

    private static BufferedImage generateVendingMachine() {
        BufferedImage img = new BufferedImage(30, 56, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);

        // Body.
        g.setColor(OUTLINE);
        g.fillRect(3, 3, 24, 51);
        g.setColor(new Color(0xB0, 0x28, 0x28));
        g.fillRect(5, 5, 20, 47);
        g.setColor(new Color(0xD8, 0x48, 0x38));
        g.fillRect(6, 6, 18, 4);

        // Glass display and rows of snacks.
        g.setColor(new Color(0x08, 0x18, 0x20));
        g.fillRect(7, 12, 16, 22);
        g.setColor(new Color(0x38, 0x78, 0x88, 190));
        g.fillRect(8, 13, 14, 20);
        Color[] snacks = {
                new Color(0xF8, 0xD8, 0x48), new Color(0x58, 0xC8, 0x68),
                new Color(0x58, 0x98, 0xE8), new Color(0xF0, 0x88, 0x38)
        };
        for (int row = 0; row < 3; row++) {
            g.setColor(new Color(0x20, 0x48, 0x50));
            g.fillRect(9, 17 + row * 6, 12, 1);
            for (int col = 0; col < 3; col++) {
                g.setColor(snacks[(row + col * 2) % snacks.length]);
                g.fillRect(10 + col * 4, 14 + row * 6, 2, 3);
            }
        }

        // Coin panel, buttons and delivery slot.
        g.setColor(new Color(0x20, 0x10, 0x10));
        g.fillRect(18, 36, 5, 10);
        g.setColor(ColorPalette.YELLOW);
        g.fillRect(20, 38, 2, 2);
        g.setColor(new Color(0x10, 0x0C, 0x0C));
        g.fillRect(7, 44, 14, 5);
        g.setColor(new Color(0x48, 0x58, 0x60));
        g.fillRect(8, 45, 12, 1);

        g.dispose();
        return img;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setStroke(new BasicStroke(1f));
        return g;
    }

    private static BufferedImage mirror(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(out);
        g.drawImage(src, src.getWidth(), 0, -src.getWidth(), src.getHeight(), null);
        g.dispose();
        return out;
    }
}

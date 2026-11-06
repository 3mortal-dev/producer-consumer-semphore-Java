package gui.game.render;

import gui.game.iso.IsoProjection;
import gui.game.scene.Camera;
import gui.game.scene.SceneId;
import gui.game.world.Actor;
import gui.game.world.ActorKind;
import gui.game.world.BedSlot;
import gui.game.world.ChairSlot;
import gui.game.world.DoctorState;
import gui.game.world.Facing;
import gui.game.world.PatientState;
import gui.game.world.QueueSlot;
import gui.game.world.WorldLayout;
import gui.game.world.WorldState;
import gui.rendering.Font8Bit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Draws the entire hospital scene with procedural 2:1 isometric artwork.
 * The renderer is read-only and runs on the EDT. Nobody outside the EDT should
 * ever call it with a live WorldState.
 */
public final class WorldRenderer {
    // Scene background
    private static final Color BG_TOP = new Color(0x08, 0x0B, 0x18);
    private static final Color BG_BOTTOM = new Color(0x11, 0x18, 0x24);
    private static final Color SLAB = new Color(0x0D, 0x0F, 0x17);
    private static final Color OUTLINE = new Color(0x0C, 0x0D, 0x14);

    // Waiting room / reception
    private static final Color WAIT_A = new Color(0x32, 0x35, 0x4C);
    private static final Color WAIT_B = new Color(0x2B, 0x2E, 0x43);
    private static final Color RUG_A = new Color(0x3C, 0x30, 0x48);
    private static final Color RUG_B = new Color(0x34, 0x2A, 0x40);
    private static final Color MAT = new Color(0x55, 0x37, 0x37);

    // Corridor
    private static final Color CORR_A = new Color(0x2C, 0x30, 0x37);
    private static final Color CORR_B = new Color(0x25, 0x29, 0x30);
    private static final Color CORR_LINE = new Color(0x8A, 0x7A, 0x36);

    // Treatment wing
    private static final Color TREAT_A = new Color(0x22, 0x3D, 0x46);
    private static final Color TREAT_B = new Color(0x1C, 0x35, 0x3E);
    private static final Color BAY_A = new Color(0x2A, 0x4B, 0x54);
    private static final Color BAY_B = new Color(0x23, 0x42, 0x4B);

    // Exterior apron
    private static final Color APRON_A = new Color(0x24, 0x25, 0x2D);
    private static final Color APRON_B = new Color(0x1E, 0x1F, 0x26);

    // Walls
    private static final Color WAIT_WALL = new Color(0x3E, 0x50, 0x78);
    private static final Color WAIT_WALL_TOP = new Color(0x6D, 0x86, 0xBC);
    private static final Color WAIT_WALL_BASE = new Color(0x27, 0x33, 0x50);
    private static final Color TREAT_WALL = new Color(0x2A, 0x58, 0x60);
    private static final Color TREAT_WALL_TOP = new Color(0x54, 0x8C, 0x96);
    private static final Color TREAT_WALL_BASE = new Color(0x1C, 0x3C, 0x44);
    private static final Color CORR_WALL = new Color(0x30, 0x3C, 0x58);
    private static final Color CORR_WALL_TOP = new Color(0x58, 0x6C, 0x94);
    private static final Color WALL_LINE = new Color(0x14, 0x1A, 0x28);

    private static final Color METAL = new Color(0xB8, 0xC2, 0xD8);
    private static final Color DOOR_GLASS = new Color(0x78, 0xC8, 0xE8, 135);
    private static final Color DOOR_GLASS_EDGE = new Color(0xC8, 0xF4, 0xFF, 210);
    private static final Color SIGN_RED = new Color(0xE8, 0x30, 0x28);
    private static final Color SIGN_WHITE = new Color(0xF8, 0xF0, 0xE0);

    // Sprite art is drawn slightly smaller than the raw cache images so the
    // characters and furniture feel like they belong to the 20x10 tiles.
    private static final double CHAR_SCALE = 0.94;
    private static final double CHAIR_SCALE = 0.72;
    private static final double BED_SCALE = 0.90;
    private static final double PROP_SCALE = 0.88;
    private static final double VENDING_SCALE = 0.66;
    private static final double COOLER_SCALE = 0.70;
    private static final double PLANT_SCALE = 0.80;
    private static final double RECEPTION_SCALE = 0.88;
    private static final double LYING_SCALE = 0.88;

    private final List<RenderItem> items = new ArrayList<>(192);
    private long randomSeed = 0x51A7C0DEL;

    public void render(Graphics2D graphics, WorldState world, Camera camera, SceneId scene) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setClip(IsoProjection.VIEW_X, IsoProjection.VIEW_Y,
                IsoProjection.VIEW_W, IsoProjection.VIEW_H);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setStroke(new BasicStroke(1f));

        drawBackdrop(g);
        drawFloorSlab(g, camera);
        drawFloor(g, camera, world);
        drawFloorOutline(g, camera);
        drawWalls(g, camera);
        drawDoor(g, camera, world);
        drawWallDecor(g, camera, world);
        drawFloorDecor(g, camera, world);
        drawEntities(g, camera, world);
        drawIdTags(g, camera, world);
        drawVignette(g);
        g.dispose();
    }

    // ------------------------------------------------------------------
    // Background and floors
    // ------------------------------------------------------------------

    private void drawBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0, IsoProjection.VIEW_Y, BG_TOP,
                0, IsoProjection.VIEW_Y + IsoProjection.VIEW_H, BG_BOTTOM));
        g.fillRect(IsoProjection.VIEW_X, IsoProjection.VIEW_Y,
                IsoProjection.VIEW_W, IsoProjection.VIEW_H);

        // Sparse pixel lights keep the empty space from feeling flat.
        randomSeed = 0x51A7C0DEL;
        g.setColor(new Color(0x21, 0x2A, 0x44));
        for (int i = 0; i < 34; i++) {
            int x = IsoProjection.VIEW_X + (int) (nextRandom() % IsoProjection.VIEW_W);
            int y = IsoProjection.VIEW_Y + (int) (nextRandom() % IsoProjection.VIEW_H);
            int size = nextRandom() % 5 == 0 ? 2 : 1;
            g.fillRect(x, y, size, size);
        }
    }

    private long nextRandom() {
        randomSeed = randomSeed * 6364136223846793005L + 1442695040888963407L;
        return (randomSeed >>> 11) & Long.MAX_VALUE;
    }

    private void drawFloorSlab(Graphics2D g, Camera camera) {
        g.setColor(SLAB);
        for (int y = 0; y < WorldLayout.GRID_H; y++) {
            for (int x = 0; x < WorldLayout.GRID_W; x++) {
                if (!WorldLayout.isFloor(x, y)) continue;
                Polygon p = IsoProjection.diamond(camera, x + 0.5, y + 0.5, -7, 1.02);
                g.fillPolygon(p);
            }
        }
    }

    private void drawFloor(Graphics2D g, Camera camera, WorldState world) {
        int[] chairBounds = chairBounds(world);
        for (int sum = 0; sum <= WorldLayout.GRID_W + WorldLayout.GRID_H; sum++) {
            for (int x = Math.max(0, sum - WorldLayout.GRID_H);
                 x <= Math.min(WorldLayout.GRID_W - 1, sum); x++) {
                int y = sum - x;
                if (!WorldLayout.isFloor(x, y)) continue;
                Polygon p = IsoProjection.diamond(camera, x + 0.5, y + 0.5, 0, 1.02);
                g.setColor(floorColor(x, y, chairBounds));
                g.fillPolygon(p);
            }
        }
    }

    private Color floorColor(int x, int y, int[] chairBounds) {
        if (x >= WorldLayout.APRON_X0 && x <= WorldLayout.APRON_X1
                && y >= WorldLayout.APRON_Y0 && y <= WorldLayout.APRON_Y1) {
            return ((x + y) & 1) == 0 ? APRON_A : APRON_B;
        }
        if (x >= WorldLayout.CORRIDOR_X0 && x <= WorldLayout.CORRIDOR_X1
                && y >= WorldLayout.CORRIDOR_Y0 && y <= WorldLayout.CORRIDOR_Y1) {
            return ((x + y) & 1) == 0 ? CORR_A : CORR_B;
        }
        if (x >= WorldLayout.TREAT_X0 && x <= WorldLayout.TREAT_X1
                && y >= WorldLayout.TREAT_Y0 && y <= WorldLayout.TREAT_Y1) {
            boolean bay = y >= 5 && y <= 9
                    && ((x >= 26 && x <= 30) || (x >= 33 && x <= 37));
            if (bay) return ((x + y) & 1) == 0 ? BAY_A : BAY_B;
            return ((x + y) & 1) == 0 ? TREAT_A : TREAT_B;
        }
        if (chairBounds != null
                && x >= chairBounds[0] - 1 && x <= chairBounds[1] + 1
                && y >= chairBounds[2] - 1 && y <= chairBounds[3] + 1) {
            return ((x + y) & 1) == 0 ? RUG_A : RUG_B;
        }
        if (x == 2 && (y == 9 || y == 10)) return MAT;
        return ((x + y) & 1) == 0 ? WAIT_A : WAIT_B;
    }

    private int[] chairBounds(WorldState world) {
        List<ChairSlot> chairs = world.slots.chairs();
        if (chairs.isEmpty()) return null;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (ChairSlot chair : chairs) {
            minX = Math.min(minX, chair.x);
            maxX = Math.max(maxX, chair.x);
            minY = Math.min(minY, chair.y);
            maxY = Math.max(maxY, chair.y);
        }
        return new int[]{minX, maxX, minY, maxY};
    }

    private void drawFloorOutline(Graphics2D g, Camera camera) {
        g.setColor(OUTLINE);
        for (int y = 0; y < WorldLayout.GRID_H; y++) {
            for (int x = 0; x < WorldLayout.GRID_W; x++) {
                if (!WorldLayout.isFloor(x, y)) continue;
                if (!WorldLayout.isFloor(x, y - 1)) {
                    drawWorldLine(g, camera, x, y, x + 1, y, 0);
                }
                if (!WorldLayout.isFloor(x - 1, y)) {
                    drawWorldLine(g, camera, x, y, x, y + 1, 0);
                }
                if (!WorldLayout.isFloor(x, y + 1)) {
                    drawWorldLine(g, camera, x, y + 1, x + 1, y + 1, 0);
                }
                if (!WorldLayout.isFloor(x + 1, y)) {
                    drawWorldLine(g, camera, x + 1, y, x + 1, y + 1, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Walls / door / wall decoration
    // ------------------------------------------------------------------

    private void drawWalls(Graphics2D g, Camera camera) {
        // Waiting room back wall + side walls (door gap on the west wall).
        drawWallX(g, camera, 2, 2, 19, 0, WorldLayout.WALL_HEIGHT, WAIT_WALL, WAIT_WALL_TOP, WAIT_WALL_BASE);
        drawWallY(g, camera, 2, 2, 8.4, 0, WorldLayout.WALL_HEIGHT, WAIT_WALL, WAIT_WALL_TOP, WAIT_WALL_BASE);
        drawWallY(g, camera, 2, 10.6, 15, 0, WorldLayout.WALL_HEIGHT, WAIT_WALL, WAIT_WALL_TOP, WAIT_WALL_BASE);
        drawWallY(g, camera, 2, 8.4, 10.6, 22, WorldLayout.WALL_HEIGHT, WAIT_WALL, WAIT_WALL_TOP, WAIT_WALL_BASE);

        // Waiting room -> corridor wall with a wide opening.
        drawWallY(g, camera, 20, 2, 6, 0, WorldLayout.WALL_HEIGHT, WAIT_WALL, WAIT_WALL_TOP, WAIT_WALL_BASE);
        drawWallY(g, camera, 20, 11, 15, 0, WorldLayout.WALL_HEIGHT, WAIT_WALL, WAIT_WALL_TOP, WAIT_WALL_BASE);

        // Corridor far wall is a low partition so it does not hide the room.
        drawWallX(g, camera, 6, 20, 24, 0, 14, CORR_WALL, CORR_WALL_TOP, WAIT_WALL_BASE);

        // Treatment wing.
        drawWallX(g, camera, 2, 25, 38, 0, WorldLayout.WALL_HEIGHT, TREAT_WALL, TREAT_WALL_TOP, TREAT_WALL_BASE);
        drawWallY(g, camera, 25, 2, 6, 0, WorldLayout.WALL_HEIGHT, TREAT_WALL, TREAT_WALL_TOP, TREAT_WALL_BASE);
        drawWallY(g, camera, 25, 11, 15, 0, WorldLayout.WALL_HEIGHT, TREAT_WALL, TREAT_WALL_TOP, TREAT_WALL_BASE);

        // Oxygen line on the treatment back wall adds an institutional detail.
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0x80, 0xB8, 0x9C));
        drawWorldLine(g, camera, 26, 2, 37, 2, 24);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(0x36, 0x66, 0x54));
        for (int x = 27; x <= 36; x += 3) {
            int px = IsoProjection.screenX(camera, x, 2);
            int py = IsoProjection.screenY(camera, x, 2, 24);
            g.fillRect(px - 1, py - 2, 3, 3);
        }
    }

    /** Wall following the +x axis at a constant y. */
    private void drawWallX(Graphics2D g, Camera camera, double y, double x0, double x1,
                           double z0, double z1, Color face, Color top, Color base) {
        Polygon wall = worldQuad(camera, x0, y, x1, y, z0, z1);
        g.setColor(face);
        g.fillPolygon(wall);
        g.setColor(WALL_LINE);
        drawWorldLine(g, camera, x0, y, x1, y, z0);
        g.setColor(top);
        drawWorldLine(g, camera, x0, y, x1, y, z1);
        // Paneling seams.
        g.setColor(new Color(0, 0, 0, 34));
        for (int x = (int) Math.ceil(x0); x <= (int) Math.floor(x1); x++) {
            drawWorldLine(g, camera, x, y, x, y, z0, z1);
        }
        g.setColor(base);
        g.fillPolygon(worldQuad(camera, x0, y, x1, y, z0, z0 + 4));
    }

    /** Wall following the +y axis at a constant x. */
    private void drawWallY(Graphics2D g, Camera camera, double x, double y0, double y1,
                           double z0, double z1, Color face, Color top, Color base) {
        Polygon wall = worldQuad(camera, x, y0, x, y1, z0, z1);
        g.setColor(face);
        g.fillPolygon(wall);
        g.setColor(WALL_LINE);
        drawWorldLine(g, camera, x, y0, x, y1, z0);
        g.setColor(top);
        drawWorldLine(g, camera, x, y0, x, y1, z1);
        g.setColor(new Color(0, 0, 0, 34));
        for (int y = (int) Math.ceil(y0); y <= (int) Math.floor(y1); y++) {
            drawWorldLine(g, camera, x, y, x, y, z0, z1);
        }
        g.setColor(base);
        g.fillPolygon(worldQuad(camera, x, y0, x, y1, z0, z0 + 4));
    }

    private void drawDoor(Graphics2D g, Camera camera, WorldState world) {
        double remaining = world.doorOpenUntilMs - world.nowMs;
        double open = remaining <= 0 ? 0.0 : Math.min(1.0, remaining / 220.0);
        open = open * open * (3 - 2 * open);

        double low = 8.4;
        double high = 10.6;
        double mid = (low + high) / 2.0;
        double half = (high - low) / 2.0;
        double leftEnd = mid - 0.06 - (half - 0.28) * open;
        double rightStart = mid + 0.06 + (half - 0.28) * open;

        // Glass panels in the west-wall doorway plane.
        drawGlassPanel(g, camera, 2, low, leftEnd, 2, 22);
        drawGlassPanel(g, camera, 2, rightStart, high, 2, 22);

        // Frame and meeting rails.
        g.setColor(METAL);
        drawWorldLine(g, camera, 2, low, 2, low, 0, 24);
        drawWorldLine(g, camera, 2, high, 2, high, 0, 24);
        drawWorldLine(g, camera, 2, low, 2, high, 22, 24);
        drawWorldLine(g, camera, 2, low, 2, high, 0, 2);
        g.setColor(new Color(0xE8, 0xF8, 0xFF, 170));
        drawWorldLine(g, camera, 2, low, 2, leftEnd, 12, 12);
        drawWorldLine(g, camera, 2, rightStart, 2, high, 12, 12);

        // Emergency sign light above the doorway.
        int sx = IsoProjection.screenX(camera, 2, mid);
        int sy = IsoProjection.screenY(camera, 2, mid, 27);
        g.setColor(new Color(0x10, 0x14, 0x20));
        g.fillRect(sx - 42, sy - 6, 84, 12);
        g.setColor(new Color(0x6E, 0x20, 0x22));
        g.drawRect(sx - 42, sy - 6, 83, 11);
        g.setColor(SIGN_RED);
        g.setFont(Font8Bit.get(8));
        String text = "EMERGENCY";
        int tw = g.getFontMetrics().stringWidth(text);
        g.drawString(text, sx - tw / 2, sy + 3);
    }

    private void drawGlassPanel(Graphics2D g, Camera camera, double x, double y0, double y1,
                                double z0, double z1) {
        if (y1 <= y0) return;
        Polygon glass = worldQuad(camera, x, y0, x, y1, z0, z1);
        g.setColor(DOOR_GLASS);
        g.fillPolygon(glass);
        g.setColor(DOOR_GLASS_EDGE);
        drawWorldLine(g, camera, x, y0, x, y0, z0, z1);
        drawWorldLine(g, camera, x, y1, x, y1, z0, z1);
        g.setColor(new Color(0xD8, 0xF8, 0xFF, 90));
        drawWorldLine(g, camera, x, y0, x, y1, z1 - 2, z1 - 2);
    }

    private void drawWallDecor(Graphics2D g, Camera camera, WorldState world) {
        // Cross sign on the waiting-room back wall.
        int crossX = IsoProjection.screenX(camera, 8.0, 2.0);
        int crossY = IsoProjection.screenY(camera, 8.0, 2.0, 19);
        g.setColor(new Color(0x10, 0x14, 0x20));
        g.fillRect(crossX - 9, crossY - 9, 18, 18);
        g.setColor(SIGN_WHITE);
        g.drawRect(crossX - 9, crossY - 9, 18, 18);
        g.setColor(SIGN_RED);
        g.fillRect(crossX - 2, crossY - 6, 4, 12);
        g.fillRect(crossX - 6, crossY - 2, 12, 4);

        // Wall-mounted television with a slowly changing pixel picture.
        int tvX = IsoProjection.screenX(camera, 4.7, 2.0);
        int tvY = IsoProjection.screenY(camera, 4.7, 2.0, 19);
        g.setColor(new Color(0x0C, 0x10, 0x18));
        g.fillRect(tvX - 12, tvY - 9, 24, 17);
        g.setColor(new Color(0x30, 0x3C, 0x54));
        g.drawRect(tvX - 12, tvY - 9, 23, 16);
        g.setColor(new Color(0x07, 0x12, 0x1A));
        g.fillRect(tvX - 9, tvY - 6, 18, 11);
        int frame = (int) (world.nowMs / 420);
        g.setColor(new Color(0x38, 0xA8, 0x98));
        g.fillRect(tvX - 7, tvY - 3 + (frame % 3), 6, 2);
        g.setColor(new Color(0x84, 0xD8, 0xC8));
        g.fillRect(tvX + 1, tvY - 4 + ((frame + 1) % 3), 5, 1);
        g.setColor(new Color(0x48, 0x6C, 0xA8));
        g.fillRect(tvX - 4, tvY + 1, 8, 1);

        // Simple wall clock.
        int clockX = IsoProjection.screenX(camera, 12.5, 2.0);
        int clockY = IsoProjection.screenY(camera, 12.5, 2.0, 19);
        g.setColor(new Color(0x10, 0x14, 0x20));
        g.fillOval(clockX - 7, clockY - 7, 14, 14);
        g.setColor(new Color(0xE8, 0xF0, 0xF8));
        g.fillOval(clockX - 6, clockY - 6, 12, 12);
        g.setColor(new Color(0x18, 0x1E, 0x28));
        double seconds = (world.nowMs / 1000.0) * Math.PI * 2 / 60.0;
        g.drawLine(clockX, clockY, clockX + (int) (Math.cos(seconds) * 4),
                clockY + (int) (Math.sin(seconds) * 4));
        g.drawLine(clockX, clockY, clockX + (int) (Math.cos(seconds / 12) * 3),
                clockY + (int) (Math.sin(seconds / 12) * 3));
        g.fillRect(clockX, clockY, 1, 1);

        // Reception label above the desk.
        int recX = IsoProjection.screenX(camera, 16.0, 2.0);
        int recY = IsoProjection.screenY(camera, 16.0, 2.0, 17);
        g.setColor(new Color(0x10, 0x14, 0x20));
        g.fillRect(recX - 42, recY - 6, 84, 12);
        g.setColor(new Color(0x5C, 0x6C, 0x8C));
        g.drawRect(recX - 42, recY - 6, 83, 11);
        g.setFont(Font8Bit.get(8));
        String rec = "RECEPTION";
        int recW = g.getFontMetrics().stringWidth(rec);
        g.setColor(SIGN_WHITE);
        g.drawString(rec, recX - recW / 2, recY + 3);

        // Treatment wing cross sign.
        int tcrossX = IsoProjection.screenX(camera, 31.5, 2.0);
        int tcrossY = IsoProjection.screenY(camera, 31.5, 2.0, 19);
        g.setColor(new Color(0x0E, 0x18, 0x1A));
        g.fillRect(tcrossX - 8, tcrossY - 8, 16, 16);
        g.setColor(new Color(0x6C, 0xC8, 0xB8));
        g.drawRect(tcrossX - 8, tcrossY - 8, 16, 16);
        g.fillRect(tcrossX - 1, tcrossY - 5, 3, 10);
        g.fillRect(tcrossX - 5, tcrossY - 1, 10, 3);
    }

    // ------------------------------------------------------------------
    // Floor markers
    // ------------------------------------------------------------------

    private void drawFloorDecor(Graphics2D g, Camera camera, WorldState world) {
        // Corridor guide stripe.
        g.setColor(CORR_LINE);
        for (int x = 21; x <= 24; x++) {
            drawWorldLine(g, camera, x + 0.15, 8.5, x + 0.85, 8.5, 0);
        }

        // Queue footprint marks on the apron so the overflow line reads clearly.
        for (QueueSlot slot : world.slots.queueSlots()) {
            int cx = IsoProjection.screenX(camera, slot.centerX(), slot.centerY());
            int cy = IsoProjection.screenY(camera, slot.centerX(), slot.centerY(), 0);
            g.setColor(slot.patientId == -1
                    ? new Color(0x56, 0x5A, 0x69, 110)
                    : new Color(0x8C, 0xC8, 0x6C, 150));
            g.fillRect(cx - 4, cy - 1, 3, 2);
            g.fillRect(cx + 1, cy - 1, 3, 2);
        }

        // Entrance arrows.
        g.setColor(new Color(0x84, 0x98, 0xBC, 90));
        for (int i = 0; i < 3; i++) {
            int x = 3 + i * 2;
            drawWorldLine(g, camera, x + 0.25, 9.5, x + 0.75, 9.2, 0);
            drawWorldLine(g, camera, x + 0.25, 9.5, x + 0.75, 9.8, 0);
        }

        // Bay divider dotted line.
        g.setColor(new Color(0x74, 0xA8, 0xB0, 120));
        for (int y = 3; y <= 12; y += 2) {
            drawWorldLine(g, camera, 31.5, y, 31.5, y + 1, 0);
        }

        // A pair of stanchions frames the outside queue.
        addStaticItem(7.6, () -> drawPropAt(g, camera, 0.45, 7.7, SpriteBank.stanchion(), PROP_SCALE));
        addStaticItem(12.4, () -> drawPropAt(g, camera, 1.55, 12.3, SpriteBank.stanchion(), PROP_SCALE));
    }

    // ------------------------------------------------------------------
    // Entities and depth sorting
    // ------------------------------------------------------------------

    private void drawEntities(Graphics2D g, Camera camera, WorldState world) {
        items.clear();

        // Chairs.
        for (ChairSlot chair : world.slots.chairs()) {
            double depth = chair.x + chair.y + 0.1;
            items.add(new RenderItem(depth, () -> drawPropAt(g, camera,
                    chair.centerX(), chair.centerY(), SpriteBank.chair(), CHAIR_SCALE)));
        }

        // Beds and their fixed equipment.
        for (BedSlot bed : world.beds) {
            double bedGx = bed.patientX;
            double bedGy = bed.bedCellY + 0.5;
            items.add(new RenderItem(bedGx + bedGy, () -> drawPropAt(g, camera,
                    bedGx, bedGy, SpriteBank.bed(), BED_SCALE)));
            items.add(new RenderItem(bed.ivX + bed.ivY, () ->
                    drawPropAt(g, camera, bed.ivX, bed.ivY, SpriteBank.ivDrip(), PROP_SCALE)));
            items.add(new RenderItem(bed.monitorX + bed.monitorY, () ->
                    drawMonitorAt(g, camera, world, bed.monitorX, bed.monitorY)));
        }

        // Reception, plants, cooler.
        items.add(new RenderItem(15.5 + 2.5, () -> drawPropAt(g, camera,
                15.5, 2.5, SpriteBank.reception(), RECEPTION_SCALE)));
        items.add(new RenderItem(18 + 2.5, () -> drawPropAt(g, camera,
                18.5, 2.5, SpriteBank.plant(), PLANT_SCALE)));
        items.add(new RenderItem(2 + 14.5, () -> drawPropAt(g, camera,
                2.5, 14.5, SpriteBank.plant(), PLANT_SCALE)));
        items.add(new RenderItem(18 + 13.5, () -> drawPropAt(g, camera,
                18.5, 13.5, SpriteBank.waterCooler(), COOLER_SCALE)));
        items.add(new RenderItem(2.4 + 5.0, () -> drawPropAt(g, camera,
                2.4, 5.0, SpriteBank.vendingMachine(), VENDING_SCALE)));

        // Actors.
        for (Actor actor : world.actorsForRendering()) {
            if (!actor.visible) continue;
            if (actor.isPatient() && actor.patientState == PatientState.GONE) continue;
            double depth = actor.gx + actor.gy + actor.z * 0.01;
            items.add(new RenderItem(depth, () -> drawActor(g, camera, world, actor)));
        }

        items.sort(Comparator.comparingDouble(RenderItem::depth));
        for (RenderItem item : items) {
            item.action().run();
        }
        items.clear();
    }

    private void drawActor(Graphics2D g, Camera camera, WorldState world, Actor actor) {
        boolean lying = actor.isPatient()
                ? (actor.patientState == PatientState.LYING
                    || (actor.patientState == PatientState.RECOVERING && actor.z > 0))
                : false;
        Facing facing = actor.facing;
        if (!actor.isPatient() && actor.roomIndex >= 0) {
            BedSlot bed = world.bedForRoom(actor.roomIndex);
            if (bed != null && (actor.doctorState == DoctorState.TREATING
                    || actor.doctorState == DoctorState.TO_BEDSIDE
                    || actor.doctorState == DoctorState.WAITING_FOR_ROOM)) {
                facing = facingToward(actor.gx, actor.gy, bed.patientX, bed.patientY, facing);
            }
        }

        if (lying) {
            int sx = IsoProjection.screenX(camera, actor.gx, actor.gy);
            int sy = IsoProjection.screenY(camera, actor.gx, actor.gy, actor.z);
            drawSprite(g, SpriteBank.lying(ActorKind.PATIENT), sx, sy,
                    camera.zoom * LYING_SCALE, Anchor.CENTER);
            return;
        }

        Pose pose = poseFor(actor);
        int frame = pose == Pose.WALK
                ? ((int) (actor.animTime * 8.0) & 1)
                : ((int) (world.nowMs / 420 + actor.id) & 1);
        BufferedImage sprite = SpriteBank.character(actor.kind, pose, facing, frame);

        int sx = IsoProjection.screenX(camera, actor.gx, actor.gy);
        int sy = IsoProjection.screenY(camera, actor.gx, actor.gy, actor.z);
        if (actor.z == 0) {
            g.setColor(new Color(0, 0, 0, 70));
            int sw = (int) Math.max(6, 12 * camera.zoom);
            int sh = (int) Math.max(2, 4 * camera.zoom);
            g.fillOval(sx - sw / 2, sy - sh / 2 + 1, sw, sh);
        }
        drawSprite(g, sprite, sx, sy, camera.zoom * CHAR_SCALE, Anchor.BOTTOM);
    }

    private Pose poseFor(Actor actor) {
        if (actor.isPatient()) {
            return switch (actor.patientState) {
                case SEATED -> Pose.SIT;
                case LYING -> Pose.LIE;
                case RECOVERING -> actor.z > 0 ? Pose.RECOVER : Pose.WALK;
                case ENTERING, QUEUED, INTAKE, STAGING -> actor.walking ? Pose.WALK : Pose.STAND;
                default -> actor.walking ? Pose.WALK : Pose.STAND;
            };
        }
        return switch (actor.doctorState) {
            case TREATING -> Pose.TREAT;
            case GESTURING -> Pose.GESTURE;
            case WAITING_FOR_ROOM -> actor.walking ? Pose.WALK : Pose.GESTURE;
            default -> actor.walking ? Pose.WALK : Pose.STAND;
        };
    }

    private static Facing facingToward(double x, double y, double tx, double ty, Facing fallback) {
        double dx = tx - x;
        double dy = ty - y;
        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) return fallback;
        if (Math.abs(dx) >= Math.abs(dy)) return dx >= 0 ? Facing.SE : Facing.NW;
        return dy >= 0 ? Facing.SW : Facing.NE;
    }

    private void drawMonitorAt(Graphics2D g, Camera camera, WorldState world,
                               double gx, double gy) {
        int sx = IsoProjection.screenX(camera, gx, gy);
        int sy = IsoProjection.screenY(camera, gx, gy, 0);
        drawSprite(g, SpriteBank.monitor(), sx, sy, camera.zoom * PROP_SCALE, Anchor.BOTTOM);

        // Animated ECG trace over the cached monitor face.
        double scale = camera.zoom * PROP_SCALE;
        int left = sx - (int) Math.round(16 * scale);
        int top = sy - (int) Math.round(46 * scale);
        int x0 = left + (int) Math.round(7 * scale);
        int y0 = top + (int) Math.round(14 * scale);
        int width = (int) Math.round(18 * scale);
        if (width < 4) return;
        g.setColor(new Color(0x1C, 0x8C, 0x50));
        g.drawLine(x0, top + (int) Math.round(7 * scale),
                x0 + width, top + (int) Math.round(7 * scale));
        g.drawLine(x0, top + (int) Math.round(20 * scale),
                x0 + width, top + (int) Math.round(20 * scale));

        int phase = (int) ((world.nowMs / 90) % Math.max(1, width));
        g.setColor(new Color(0x54, 0xFF, 0x9C));
        for (int i = 0; i < width - 1; i++) {
            double t = (i - phase) / (double) width;
            int yy = ecgOffset(t * 2.0 * Math.PI * 2.0);
            g.drawLine(x0 + i, y0 + yy, x0 + i + 1, y0 + ecgOffset((i + 1 - phase)
                    / (double) width * 2.0 * Math.PI * 2.0));
        }
        if ((world.nowMs / 500) % 2 == 0) {
            g.setColor(new Color(0xFF, 0x58, 0x58));
            g.fillRect(x0 + width + 2, top + (int) Math.round(8 * scale), 2, 2);
        }
    }

    private static int ecgOffset(double t) {
        double s = Math.sin(t);
        double spike = Math.exp(-Math.pow((t % (Math.PI * 2)) - 4.2, 2) * 18);
        return (int) Math.round(-spike * 4 + s * 1.2);
    }

    // ------------------------------------------------------------------
    // ID tags
    // ------------------------------------------------------------------

    private void drawIdTags(Graphics2D g, Camera camera, WorldState world) {
        List<Rectangle> placed = new ArrayList<>(world.patients.size() + world.doctors.size());
        for (Actor actor : world.actorsForRendering()) {
            if (!actor.visible) continue;
            if (actor.isPatient() && actor.patientState == PatientState.GONE) continue;

            int sx = IsoProjection.screenX(camera, actor.gx, actor.gy);
            int sy = IsoProjection.screenY(camera, actor.gx, actor.gy, actor.z);
            boolean lying = actor.isPatient()
                    && (actor.patientState == PatientState.LYING
                        || (actor.patientState == PatientState.RECOVERING && actor.z > 0));
            int spriteHeight = lying
                    ? (int) Math.round(24 * LYING_SCALE * camera.zoom)
                    : (int) Math.round(36 * CHAR_SCALE * camera.zoom);
            int top = lying ? sy - spriteHeight / 2 : sy - spriteHeight;

            String label = (actor.isPatient() ? "P" : "D") + actor.id;
            int w = Font8Bit.stringWidth(g, label, 8) + 6;
            int h = 12;
            Rectangle tag = new Rectangle(sx - w / 2, top - h - 2, w, h);
            if (tag.y < IsoProjection.VIEW_Y + 1) tag.y = IsoProjection.VIEW_Y + 1;

            // Stack tags upward when sprites crowd together. The actors may be
            // walking through the same doorway, but their IDs should stay
            // readable instead of overprinting each other.
            for (int attempt = 0; attempt < 16 && overlapsAny(placed, tag); attempt++) {
                tag.y -= h + 1;
                if (tag.y < IsoProjection.VIEW_Y + 1) {
                    tag.y = IsoProjection.VIEW_Y + 1;
                    break;
                }
            }

            g.setColor(new Color(0x04, 0x06, 0x0C, 205));
            g.fillRect(tag.x, tag.y, tag.width, tag.height);
            g.setColor(actor.isPatient()
                    ? new Color(0x9C, 0xD8, 0xF0)
                    : new Color(0xFF, 0xC8, 0x60));
            g.drawRect(tag.x, tag.y, tag.width - 1, tag.height - 1);
            g.setFont(Font8Bit.get(8));
            g.setColor(actor.isPatient() ? Color.WHITE : new Color(0xFF, 0xE8, 0xB0));
            int tw = g.getFontMetrics().stringWidth(label);
            g.drawString(label, tag.x + (tag.width - tw) / 2, tag.y + 9);
            placed.add(tag);
        }
    }

    private static boolean overlapsAny(List<Rectangle> placed, Rectangle candidate) {
        for (Rectangle rect : placed) {
            if (rect.intersects(candidate)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Drawing helpers
    // ------------------------------------------------------------------

    private void drawVignette(Graphics2D g) {
        // Thin dark edges sell the little CRT / arcade-screen feel.
        g.setColor(new Color(0, 0, 0, 28));
        g.fillRect(IsoProjection.VIEW_X, IsoProjection.VIEW_Y,
                IsoProjection.VIEW_W, 3);
        g.fillRect(IsoProjection.VIEW_X, IsoProjection.VIEW_Y + IsoProjection.VIEW_H - 3,
                IsoProjection.VIEW_W, 3);
        g.fillRect(IsoProjection.VIEW_X, IsoProjection.VIEW_Y, 3,
                IsoProjection.VIEW_H);
        g.fillRect(IsoProjection.VIEW_X + IsoProjection.VIEW_W - 3, IsoProjection.VIEW_Y,
                3, IsoProjection.VIEW_H);
    }

    private void addStaticItem(double depth, Runnable action) {
        items.add(new RenderItem(depth, action));
        items.sort(Comparator.comparingDouble(RenderItem::depth));
        for (RenderItem item : items) item.action().run();
        items.clear();
    }

    private void drawPropAt(Graphics2D g, Camera camera, double gx, double gy,
                            BufferedImage sprite, double artScale) {
        int sx = IsoProjection.screenX(camera, gx, gy);
        int sy = IsoProjection.screenY(camera, gx, gy, 0);
        drawSprite(g, sprite, sx, sy, camera.zoom * artScale, Anchor.BOTTOM);
    }

    private enum Anchor { BOTTOM, CENTER }

    private static void drawSprite(Graphics2D g, BufferedImage img,
                                   double anchorX, double anchorY,
                                   double scale, Anchor anchor) {
        int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(img.getHeight() * scale));
        int x = (int) Math.round(anchorX - w / 2.0);
        int y = (int) Math.round(anchorY - (anchor == Anchor.BOTTOM ? h : h / 2.0));
        g.drawImage(img, x, y, w, h, null);
    }

    private static Polygon worldQuad(Camera camera,
                                     double x0, double y0, double x1, double y1,
                                     double z0, double z1) {
        return new Polygon(
                new int[]{
                        IsoProjection.screenX(camera, x0, y0),
                        IsoProjection.screenX(camera, x1, y1),
                        IsoProjection.screenX(camera, x1, y1),
                        IsoProjection.screenX(camera, x0, y0)},
                new int[]{
                        IsoProjection.screenY(camera, x0, y0, z0),
                        IsoProjection.screenY(camera, x1, y1, z0),
                        IsoProjection.screenY(camera, x1, y1, z1),
                        IsoProjection.screenY(camera, x0, y0, z1)},
                4);
    }

    private static void drawWorldLine(Graphics2D g, Camera camera,
                                      double x0, double y0, double x1, double y1, double z) {
        g.drawLine(IsoProjection.screenX(camera, x0, y0),
                IsoProjection.screenY(camera, x0, y0, z),
                IsoProjection.screenX(camera, x1, y1),
                IsoProjection.screenY(camera, x1, y1, z));
    }

    private static void drawWorldLine(Graphics2D g, Camera camera,
                                      double x0, double y0, double x1, double y1,
                                      double z0, double z1) {
        g.drawLine(IsoProjection.screenX(camera, x0, y0),
                IsoProjection.screenY(camera, x0, y0, z0),
                IsoProjection.screenX(camera, x1, y1),
                IsoProjection.screenY(camera, x1, y1, z1));
    }

    private record RenderItem(double depth, Runnable action) {}
}

package gui.game.iso;

import gui.game.scene.Camera;
import gui.game.world.WorldLayout;

import java.awt.Polygon;

/**
 * Standard 2:1 isometric projection and its inverse.
 */
public final class IsoProjection {
    private IsoProjection() {}

    public static final int VIEW_X = 0;
    // Must match HudRenderer.TOP_H / BOTTOM_Y: 44 + 296 + 20 = 360.
    public static final int VIEW_Y = 44;
    public static final int VIEW_W = 640;
    public static final int VIEW_H = 296;
    public static final int VIEW_CX = VIEW_W / 2;
    public static final int VIEW_CY = VIEW_Y + VIEW_H / 2;

    public static double isoX(double gx, double gy) {
        return (gx - gy) * (WorldLayout.TILE_W / 2.0);
    }

    public static double isoY(double gx, double gy, double z) {
        return (gx + gy) * (WorldLayout.TILE_H / 2.0) - z;
    }

    public static double focusIsoX(Camera camera) {
        return isoX(camera.focusGx, camera.focusGy);
    }

    public static double focusIsoY(Camera camera) {
        return isoY(camera.focusGx, camera.focusGy, 0);
    }

    public static int screenX(Camera camera, double gx, double gy) {
        double x = VIEW_CX + (isoX(gx, gy) - focusIsoX(camera)) * camera.zoom;
        return (int) Math.round(x);
    }

    public static int screenY(Camera camera, double gx, double gy, double z) {
        double y = VIEW_CY + (isoY(gx, gy, z) - focusIsoY(camera)) * camera.zoom;
        return (int) Math.round(y);
    }

    /** Inverse projection for panning and mouse picking at floor level. */
    public static double[] screenToWorld(Camera camera, double screenX, double screenY) {
        double ix = (screenX - VIEW_CX) / camera.zoom + focusIsoX(camera);
        double iy = (screenY - VIEW_CY) / camera.zoom + focusIsoY(camera);
        double halfW = WorldLayout.TILE_W / 2.0;
        double halfH = WorldLayout.TILE_H / 2.0;
        double gx = (ix / halfW + iy / halfH) / 2.0;
        double gy = (iy / halfH - ix / halfW) / 2.0;
        return new double[]{gx, gy};
    }

    /** Screen-space diamond for a floor tile (or elevated object base). */
    public static Polygon diamond(Camera camera, double gx, double gy, int z, double inset) {
        int cx = screenX(camera, gx, gy);
        int cy = screenY(camera, gx, gy, z);
        double halfW = (WorldLayout.TILE_W / 2.0) * camera.zoom * inset;
        double halfH = (WorldLayout.TILE_H / 2.0) * camera.zoom * inset;
        return new Polygon(
                new int[]{cx, (int) Math.round(cx + halfW), cx, (int) Math.round(cx - halfW)},
                new int[]{cy - (int) Math.round(halfH), cy, cy + (int) Math.round(halfH), cy},
                4);
    }

    public static double fitZoom(int gridW, int gridH, int viewW, int viewH, int padding) {
        double mapW = (gridW + gridH) * (WorldLayout.TILE_W / 2.0);
        double mapH = (gridW + gridH) * (WorldLayout.TILE_H / 2.0) + WorldLayout.WALL_HEIGHT;
        double zx = (viewW - padding * 2.0) / mapW;
        double zy = (viewH - padding * 2.0) / mapH;
        return Math.max(0.4, Math.min(zx, zy));
    }

    /** Returns true when a world-space rectangle could intersect the game viewport. */
    public static boolean onScreen(Camera camera, double gx, double gy, int margin) {
        int x = screenX(camera, gx, gy);
        int y = screenY(camera, gx, gy, 0);
        return x >= VIEW_X - margin && x <= VIEW_X + VIEW_W + margin
                && y >= VIEW_Y - margin && y <= VIEW_Y + VIEW_H + margin;
    }
}

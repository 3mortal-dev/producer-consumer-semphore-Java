package gui.game.scene;

import gui.game.iso.IsoProjection;
import gui.game.world.WorldLayout;

/**
 * Owns the camera, scene presets, eased transitions and manual pan/zoom.
 * All methods run on the EDT.
 */
public final class CameraManager {
    private static final double TRANSITION_SECONDS = 0.38;

    private final Camera camera;
    private SceneId activeScene = SceneId.WAITING_ROOM;

    private boolean tweening;
    private double tweenElapsed;
    private double fromX, fromY, fromZoom;
    private double toX, toY, toZoom;

    public CameraManager() {
        camera = new Camera(10.5, 9.0, waitingZoom());
    }

    public Camera camera() {
        return camera;
    }

    public SceneId activeScene() {
        return activeScene;
    }

    public boolean isTweening() {
        return tweening;
    }

    public void setScene(SceneId scene) {
        activeScene = scene;
        fromX = camera.focusGx;
        fromY = camera.focusGy;
        fromZoom = camera.zoom;

        switch (scene) {
            case WAITING_ROOM -> {
                toX = 10.5;
                toY = 9.0;
                toZoom = waitingZoom();
            }
            case TREATMENT_WING -> {
                toX = 31.5;
                toY = 9.0;
                toZoom = treatmentZoom();
            }
            case FULL_FLOOR -> {
                // Fit the actual floor plan (not the whole 40x18 bounding box):
                // the waiting room and treatment wing both fill the viewport
                // with a little air above and below.
                toX = 18.5;
                toY = 6.5;
                toZoom = 1.0;
            }
        }
        tweening = true;
        tweenElapsed = 0;
    }

    public void zoomBy(double factor) {
        cancelTween();
        camera.zoom = clampZoom(camera.zoom * factor);
    }

    public void pan(double screenDx, double screenDy) {
        cancelTween();
        // Drag right -> move the camera left over the map.
        double isoDx = -screenDx / camera.zoom;
        double isoDy = -screenDy / camera.zoom;
        double halfW = WorldLayout.TILE_W / 2.0;
        double halfH = WorldLayout.TILE_H / 2.0;
        double dgx = (isoDx / halfW + isoDy / halfH) / 2.0;
        double dgy = (isoDy / halfH - isoDx / halfW) / 2.0;
        camera.focusGx = clampX(camera.focusGx + dgx);
        camera.focusGy = clampY(camera.focusGy + dgy);
    }

    public void resetView() {
        setScene(activeScene);
    }

    public void update(double dtSeconds) {
        if (!tweening) return;
        tweenElapsed += dtSeconds;
        double t = Math.min(1.0, tweenElapsed / TRANSITION_SECONDS);
        double eased = t * t * (3 - 2 * t); // smoothstep
        camera.focusGx = fromX + (toX - fromX) * eased;
        camera.focusGy = fromY + (toY - fromY) * eased;
        camera.zoom = fromZoom + (toZoom - fromZoom) * eased;
        if (t >= 1.0) {
            tweening = false;
            camera.focusGx = toX;
            camera.focusGy = toY;
            camera.zoom = toZoom;
        }
    }

    private void cancelTween() {
        tweening = false;
    }

    private double waitingZoom() {
        return 1.55;
    }

    private double treatmentZoom() {
        return 1.55;
    }

    private double clampZoom(double zoom) {
        return Math.max(0.45, Math.min(2.8, zoom));
    }

    private double clampX(double gx) {
        return Math.max(-4, Math.min(WorldLayout.GRID_W + 4, gx));
    }

    private double clampY(double gy) {
        return Math.max(-4, Math.min(WorldLayout.GRID_H + 4, gy));
    }
}

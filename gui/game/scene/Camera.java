package gui.game.scene;

/** Camera focus in tile space plus a zoom factor. */
public final class Camera {
    public double focusGx;
    public double focusGy;
    public double zoom;

    public Camera(double focusGx, double focusGy, double zoom) {
        this.focusGx = focusGx;
        this.focusGy = focusGy;
        this.zoom = zoom;
    }

    public Camera copy() {
        return new Camera(focusGx, focusGy, zoom);
    }
}

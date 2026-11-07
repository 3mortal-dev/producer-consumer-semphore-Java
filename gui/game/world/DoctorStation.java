package gui.game.world;

/** Where an idle doctor stands. */
public final class DoctorStation {
    public final int index;
    public final int x;
    public final int y;

    public DoctorStation(int index, int x, int y) {
        this.index = index;
        this.x = x;
        this.y = y;
    }

    public double centerX() { return x + 0.5; }
    public double centerY() { return y + 0.5; }
}

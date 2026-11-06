package gui.game.world;

/** One seat in the isometric waiting room. */
public final class ChairSlot {
    public final int index;
    public final int x;
    public final int y;
    public int patientId = -1;

    public ChairSlot(int index, int x, int y) {
        this.index = index;
        this.x = x;
        this.y = y;
    }

    public double centerX() { return x + 0.5; }
    public double centerY() { return y + 0.5; }
}

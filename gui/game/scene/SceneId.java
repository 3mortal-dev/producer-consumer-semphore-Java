package gui.game.scene;

public enum SceneId {
    // Short tab labels: at the minimum readable pixel size the full names
    // ("[WAITING ROOM]" etc.) leave no room for the speed/pause controls.
    WAITING_ROOM("WAITING"),
    TREATMENT_WING("TREATMENT"),
    FULL_FLOOR("FULL FLOOR");

    public final String label;

    SceneId(String label) {
        this.label = label;
    }
}

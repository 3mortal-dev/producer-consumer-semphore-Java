package gui.events;

/**
 * Treatment room state events.
 */
public record RoomEvent(
    int roomIndex,  // 0 or 1
    RoomAction action,
    int doctorId,   // -1 if not applicable
    int patientId,  // -1 if not applicable
    int timestamp
) implements SimulationEvent {

    public enum RoomAction {
        OCCUPIED,
        RELEASED
    }

    @Override
    public String toString() {
        String d = doctorId >= 0 ? " D" + doctorId : "";
        String p = patientId >= 0 ? " P" + patientId : "";
        return String.format("[%d] Room %d%s%s: %s", timestamp, roomIndex + 1, d, p, action);
    }
}
package gui.events;

/**
 * Doctor lifecycle events.
 */
public record DoctorEvent(
    int doctorId,
    DoctorAction action,
    int patientId,  // -1 if not applicable
    int timestamp
) implements SimulationEvent {

    public enum DoctorAction {
        IDLE,
        ASSIGNED_PATIENT,
        TRYING_ACQUIRE_ROOM,
        ACQUIRED_ROOM,
        TREATING,
        RELEASED_ROOM,
        COULD_NOT_ACQUIRE,
        SENDING_BACK,
        AVAILABLE
    }

    @Override
    public String toString() {
        String p = patientId >= 0 ? " P" + patientId : "";
        return String.format("[%d] Doctor %d%s: %s", timestamp, doctorId, p, action);
    }
}
package gui.events;

/**
 * Patient lifecycle events.
 */
public record PatientEvent(
    int patientId,
    PatientAction action,
    int timestamp
) implements SimulationEvent {

    public enum PatientAction {
        ARRIVED,
        ENTERED_WAITING_ROOM,
        WAITING_OUTSIDE,
        TREATMENT_STARTED,
        TREATMENT_FINISHED,
        SENT_BACK,
        LEFT
    }

    @Override
    public String toString() {
        return String.format("[%d] Patient %d: %s", timestamp, patientId, action);
    }
}
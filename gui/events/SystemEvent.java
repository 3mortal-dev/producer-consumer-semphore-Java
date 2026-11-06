package gui.events;

/**
 * System-level events (simulation start/end, errors, etc.)
 */
public record SystemEvent(
    String message,
    int timestamp
) implements SimulationEvent {

    @Override
    public String toString() {
        return String.format("[%d] SYSTEM: %s", timestamp, message);
    }
}
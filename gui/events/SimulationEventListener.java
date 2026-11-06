package gui.events;

/**
 * Listener interface for simulation events.
 * Implementations should be thread-safe as events may be posted from simulation threads.
 */
@FunctionalInterface
public interface SimulationEventListener {
    void onEvent(SimulationEvent event);
}
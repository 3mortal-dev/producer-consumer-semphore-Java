package gui.events;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton event broadcaster for simulation events.
 * Uses lock-free data structures for high-throughput event passing.
 * Listeners are notified on the posting thread - they must handle EDT dispatch themselves.
 */
public final class SimulationEventBroadcaster {
    private static final SimulationEventBroadcaster INSTANCE = new SimulationEventBroadcaster();

    private final ConcurrentLinkedQueue<SimulationEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<SimulationEventListener> listeners = new CopyOnWriteArrayList<>();

    private SimulationEventBroadcaster() {}

    public static SimulationEventBroadcaster getInstance() {
        return INSTANCE;
    }

    /**
     * Post an event to all registered listeners.
     * Non-blocking, O(1) enqueue + fan-out.
     * Called from simulation threads.
     */
    public void post(SimulationEvent event) {
        eventQueue.add(event);
        // Fan out to listeners (they handle EDT dispatch)
        for (SimulationEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                System.err.println("Error in simulation event listener: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Register a listener. Thread-safe.
     */
    public void register(SimulationEventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister a listener. Thread-safe.
     */
    public void unregister(SimulationEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Get pending events (for testing/debugging).
     */
    public ConcurrentLinkedQueue<SimulationEvent> getEventQueue() {
        return eventQueue;
    }

    /**
     * Clear all listeners (for shutdown).
     */
    public void clear() {
        listeners.clear();
        eventQueue.clear();
    }
}
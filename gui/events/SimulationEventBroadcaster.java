package gui.events;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton event broadcaster for simulation events.
 * Uses lock-free data structures for high-throughput event passing.
 * Listeners are notified on the posting thread - they must handle EDT dispatch themselves.
 */
public final class SimulationEventBroadcaster {
    private static final SimulationEventBroadcaster INSTANCE = new SimulationEventBroadcaster();
    private static final int DIAGNOSTIC_CAPACITY = 2048;

    // Bounded, non-blocking diagnostic history. Production listeners receive the
    // event directly; this queue exists only for tests/debugging and must never
    // grow without bound while a long simulation runs.
    private final Queue<SimulationEvent> eventQueue = new ArrayBlockingQueue<>(DIAGNOSTIC_CAPACITY);
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
        eventQueue.offer(event); // drop-newest if the diagnostic buffer is full
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
     * Get pending diagnostic events (for testing/debugging).
     */
    public Queue<SimulationEvent> getEventQueue() {
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
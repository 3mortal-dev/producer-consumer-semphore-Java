package gui.game;

import gui.events.SimulationEvent;
import gui.events.SimulationEventListener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Non-blocking bridge between simulation threads and the EDT.
 *
 * Backend threads only call {@link #onEvent(SimulationEvent)}, which performs a
 * single lock-free queue insertion. The EDT drains the queue once per frame and
 * applies the events to the world model. No Swing access happens here.
 */
public final class GameEventBridge implements SimulationEventListener {
    private final ConcurrentLinkedQueue<SimulationEvent> queue = new ConcurrentLinkedQueue<>();
    private volatile long receivedCount;

    @Override
    public void onEvent(SimulationEvent event) {
        queue.add(event);
        receivedCount++;
    }

    /** Drains all pending events on the EDT. Returns the number processed. */
    public int drain(Consumer<SimulationEvent> consumer) {
        int count = 0;
        SimulationEvent event;
        while ((event = queue.poll()) != null) {
            consumer.accept(event);
            count++;
        }
        return count;
    }

    public void clear() {
        queue.clear();
    }

    public int backlog() {
        return queue.size();
    }

    public long receivedCount() {
        return receivedCount;
    }
}

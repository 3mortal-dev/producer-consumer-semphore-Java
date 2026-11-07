package gui.game;

import gui.events.SimulationEvent;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/**
 * Visual pacing between the backend and the world model.
 *
 * The backend posts events as fast as the simulation runs; this queue decides
 * when the EDT is allowed to see them. Three modes:
 * <ul>
 *   <li>OFF — every queued event is applied on the next frame (original behaviour).</li>
 *   <li>DELAY — each event is held for {@code delayMs} and then spaced out, so a
 *       burst of arrivals appears as a readable sequence instead of one instant.</li>
 *   <li>STEP — nothing is released until {@link #step} is called, one event per
 *       call. Ideal for following a single patient's journey.</li>
 * </ul>
 *
 * All methods run on the EDT; the backend never sees this class. The clock is
 * the world's presentation time ({@code WorldState.nowMs}), so pausing the world
 * also freezes playback.
 */
public final class EventPlaybackQueue {
    /** How the user wants events paced. */
    public enum Mode { OFF, DELAY, STEP }

    /** Adjustable delay choices offered in the UI, in milliseconds. */
    public static final int[] DELAY_CHOICES_MS = {300, 700, 1200, 2000};
    public static final int DEFAULT_DELAY_INDEX = 1; // 700 ms

    private record Slot(SimulationEvent event, long releaseAtMs) {}

    private final ArrayDeque<Slot> queue = new ArrayDeque<>();
    private Mode mode = Mode.DELAY;
    private int delayIndex = DEFAULT_DELAY_INDEX;
    private long lastSlotMs = Long.MIN_VALUE;

    public void enqueue(SimulationEvent event, long nowMs) {
        if (mode == Mode.OFF) {
            queue.addLast(new Slot(event, nowMs));
            return;
        }
        long delay = delayMs();
        long spacing = spacingMs();
        // Space slots out so a burst becomes a sequence. When the backlog is
        // huge the spacing halves so the visual lag stays bounded.
        if (queue.size() > 60) spacing /= 2;
        long slot = nowMs + delay;
        if (lastSlotMs != Long.MIN_VALUE) {
            slot = Math.max(slot, lastSlotMs + spacing);
        }
        lastSlotMs = slot;
        queue.addLast(new Slot(event, slot));
    }

    /**
     * Releases every event whose time has come (DELAY), or everything (OFF).
     * In STEP mode this releases nothing; use {@link #step} instead.
     * Returns the number of events applied.
     */
    public int release(long nowMs, Consumer<SimulationEvent> consumer) {
        if (mode == Mode.STEP) return 0;
        int count = 0;
        if (mode == Mode.OFF) {
            Slot slot;
            while ((slot = queue.pollFirst()) != null) {
                consumer.accept(slot.event());
                count++;
            }
            lastSlotMs = Long.MIN_VALUE;
            return count;
        }
        Slot head;
        while ((head = queue.peekFirst()) != null && head.releaseAtMs() <= nowMs) {
            queue.pollFirst();
            consumer.accept(head.event());
            count++;
        }
        return count;
    }

    /** Releases exactly one queued event. Only meaningful in STEP mode. */
    public int step(Consumer<SimulationEvent> consumer) {
        Slot slot = queue.pollFirst();
        if (slot == null) return 0;
        consumer.accept(slot.event());
        return 1;
    }

    public int backlog() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
        lastSlotMs = Long.MIN_VALUE;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode newMode) {
        mode = newMode == null ? Mode.DELAY : newMode;
        if (mode == Mode.OFF) lastSlotMs = Long.MIN_VALUE;
    }

    public int delayMs() {
        return DELAY_CHOICES_MS[Math.max(0, Math.min(delayIndex, DELAY_CHOICES_MS.length - 1))];
    }

    /** Gap between consecutive releases in DELAY mode: delay/3, at least 60 ms. */
    public long spacingMs() {
        return Math.max(60, delayMs() / 3);
    }

    public int delayIndex() {
        return delayIndex;
    }

    public void setDelayIndex(int index) {
        delayIndex = Math.floorMod(index, DELAY_CHOICES_MS.length);
    }

    public void cycleDelay() {
        setDelayIndex(delayIndex + 1);
    }

    /** Short label for HUD/title chips, e.g. "DELAY 0.7S". */
    public String delayLabel() {
        return String.format("%.1fS", delayMs() / 1000.0);
    }
}

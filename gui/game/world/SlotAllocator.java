package gui.game.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** EDT-confined deterministic assignment of chairs and overflow queue slots. */
public final class SlotAllocator {
    private final List<ChairSlot> chairs;
    private final List<QueueSlot> queueSlots;
    private final Map<Integer, ChairSlot> chairByPatient = new HashMap<>();
    private final Map<Integer, QueueSlot> queueByPatient = new HashMap<>();

    public SlotAllocator(List<ChairSlot> chairs, List<QueueSlot> queueSlots) {
        this.chairs = chairs;
        this.queueSlots = queueSlots;
    }

    public List<ChairSlot> chairs() { return chairs; }
    public List<QueueSlot> queueSlots() { return queueSlots; }

    public ChairSlot assignChair(int patientId) {
        ChairSlot existing = chairByPatient.get(patientId);
        if (existing != null) return existing;
        for (ChairSlot chair : chairs) {
            if (chair.patientId == -1) {
                chair.patientId = patientId;
                chairByPatient.put(patientId, chair);
                return chair;
            }
        }
        return null;
    }

    public QueueSlot assignQueue(int patientId) {
        QueueSlot existing = queueByPatient.get(patientId);
        if (existing != null) return existing;
        for (QueueSlot slot : queueSlots) {
            if (slot.patientId == -1) {
                slot.patientId = patientId;
                queueByPatient.put(patientId, slot);
                return slot;
            }
        }
        return null;
    }

    public ChairSlot chairFor(int patientId) {
        return chairByPatient.get(patientId);
    }

    public QueueSlot queueFor(int patientId) {
        return queueByPatient.get(patientId);
    }

    public void releaseChair(int patientId) {
        ChairSlot chair = chairByPatient.remove(patientId);
        if (chair != null) chair.patientId = -1;
    }

    public void releaseQueue(int patientId) {
        QueueSlot slot = queueByPatient.remove(patientId);
        if (slot != null) slot.patientId = -1;
    }

    public void releaseAll(int patientId) {
        releaseChair(patientId);
        releaseQueue(patientId);
    }

    public int chairsOccupied() {
        int count = 0;
        for (ChairSlot chair : chairs) {
            if (chair.patientId != -1) count++;
        }
        return count;
    }

    public int queueOccupied() {
        int count = 0;
        for (QueueSlot slot : queueSlots) {
            if (slot.patientId != -1) count++;
        }
        return count;
    }
}

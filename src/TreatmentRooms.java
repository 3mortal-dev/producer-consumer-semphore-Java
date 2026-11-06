package src;

// those also will be represented as a Semaphore
import java.util.Arrays;

public class TreatmentRooms {
  private final Semaphore rooms;
  private final Integer ROOM_SIZE = 2;
  private final int[] roomDoctor = new int[ROOM_SIZE];   // -1 if free
  private final int[] roomPatient = new int[ROOM_SIZE];  // -1 if free

  public TreatmentRooms() {
    rooms = new Semaphore(ROOM_SIZE);
    Arrays.fill(roomDoctor, -1);
    Arrays.fill(roomPatient, -1);
  }

  /**
   * Try to acquire a treatment room with timeout.
   * Returns room index (0 or 1) on success, -1 on timeout.
   *
   * IMPORTANT: the potentially blocking semaphore wait happens OUTSIDE the
   * synchronized block. Otherwise a doctor waiting for a free room would hold
   * the TreatmentRooms monitor and block another doctor from releasing a room.
   */
  public int tryAcquireRoom(int doctorId, int patientId, int timeoutMs) throws InterruptedException {
    if (!rooms.tryAcquire(timeoutMs)) {
      return -1;
    }
    synchronized (this) {
      for (int i = 0; i < ROOM_SIZE; i++) {
        if (roomDoctor[i] == -1) {
          roomDoctor[i] = doctorId;
          roomPatient[i] = patientId;
          EventLogger.roomOccupied(i, doctorId, patientId);
          return i;
        }
      }
    }
    // Should not happen if semaphore works correctly
    rooms.release(); // return permit
    return -1;
  }

  public void releaseRoom(int roomIndex, int doctorId, int patientId) {
    synchronized (this) {
      if (roomIndex >= 0 && roomIndex < ROOM_SIZE) {
        roomDoctor[roomIndex] = -1;
        roomPatient[roomIndex] = -1;
        EventLogger.roomReleased(roomIndex, doctorId, patientId);
      }
    }
    rooms.release();
  }

  // Legacy methods for compatibility
  public Boolean tryAcquire(Integer time) throws InterruptedException {
    return rooms.tryAcquire(time);
  }

  public void acquire() throws InterruptedException {
    rooms.acquire();
  }

  public void release() {
    rooms.release();
  }

  public synchronized int getRoomDoctor(int roomIndex) {
    return roomDoctor[roomIndex];
  }

  public synchronized int getRoomPatient(int roomIndex) {
    return roomPatient[roomIndex];
  }

  public int getRoomCount() {
    return ROOM_SIZE;
  }
}

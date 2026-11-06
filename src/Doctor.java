package src;
import java.util.Random;

public class Doctor extends Thread {
  private static final Integer ROOM_TIMEOUT = 3000;
  private static final Integer MIN_TREATMENT = 2000;
  private static final Integer MAX_TREATMENT = 5000;

  private final Integer id;
  private final WaitingRoom waitingRoom;
  private final TreatmentRooms treatmentRooms;

  public Doctor(Integer id, WaitingRoom waitingRoom, TreatmentRooms treatmentRooms) {
    this.id = id;
    this.waitingRoom = waitingRoom;
    this.treatmentRooms = treatmentRooms;
  }

  @Override
  public void run() {
    try {
      while (true) {
        Patient p = waitingRoom.consume();

        EventLogger.doctorAssignedPatient(id, p.getPId());
        EventLogger.doctorTryingAcquireRoom(id, p.getPId());

        float speed = SimulationConfig.getSpeed();
        int timeout = Math.max(1, (int) (ROOM_TIMEOUT / Math.max(0.1f, speed)));
        int roomIndex = treatmentRooms.tryAcquireRoom(id, p.getPId(), timeout);
        if (roomIndex >= 0) {
          EventLogger.doctorAcquiredRoom(id, p.getPId());
          EventLogger.doctorTreating(id, p.getPId());
          int treatmentMs = new Random().nextInt(MAX_TREATMENT - MIN_TREATMENT + 1) + MIN_TREATMENT;
          long scaledTreatment = (long) (treatmentMs / Math.max(0.1f, speed));
          Thread.sleep(scaledTreatment);
          treatmentRooms.releaseRoom(roomIndex, id, p.getPId());
          EventLogger.doctorReleasedRoom(id, p.getPId());
          p.setIsTreated(true);
          p.release();
          EventLogger.doctorAvailable(id);
        }
        // if the doctor couldn't get a room under 3 seconds
        else {
          EventLogger.doctorCouldNotAcquire(id, p.getPId());
          EventLogger.doctorSendingBack(id, p.getPId());
          p.release();
          EventLogger.doctorAvailable(id);
        }

      }
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public String toString() {
    return "Doctor" + id;
  }
}

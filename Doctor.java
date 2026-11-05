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
        // TODO: assign the doctor to the patient
        // TODO: try to acquire
        if (treatmentRooms.tryAcquire(ROOM_TIMEOUT)) {
          // TODO: treatment acquired
          // TODO: treatment start
          Thread.sleep(new Random().nextInt(MAX_TREATMENT - MIN_TREATMENT + 1) + MIN_TREATMENT);
          // TODO: treatment end
          p.setIsTreated(true);
          p.release();
          // TODO: release room
          // TODO: Dr. is available
        }
        // if the doctor couldn't get a room under 3 seconds
        else {
          // TODO: couldn't acquire a room
          // TODO: release the patient with isTreated still false
          p.release();
          // TODO: Dr. is available
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

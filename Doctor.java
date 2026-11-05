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
      Random r = new Random();
      while (true) {
        Patient p = waitingRoom.consume();
        // assign the doctor to the patient
        if (true /* tryAcquire */) {
          // TODO: implement tryAcquire
          treatmentRooms.acquire();
          // treatment acquired
          // treatment start
          Thread.sleep(new Random().nextInt(MAX_TREATMENT - MIN_TREATMENT + 1) + MIN_TREATMENT);
          // treatment end
          p.setIsTreated(true);
          p.release();
          // release room
          // Dr. is available
        }
        // if the doctor couldn't get a room under 3 seconds
        else {
          // couldn't acquire a room
          // release the patient with isTreated still false
          p.release();
          // Dr. is available
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

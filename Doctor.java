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

        System.out.println(this + " is assigned to " + p);
        System.out.println(this + " is trying to acquire a treatement room ");

        if (treatmentRooms.tryAcquire(ROOM_TIMEOUT)) {
          System.out.println(this + " acquired a treatement room");
          System.out.println(this + " begins treating " + p);
          Thread.sleep(new Random().nextInt(MAX_TREATMENT - MIN_TREATMENT + 1) + MIN_TREATMENT);
          System.out.println(this + " finished treating " + p);
          treatmentRooms.release();
          System.out.println(this + " released the treatment room");
          p.setIsTreated(true);
          p.release();
          System.out.println(this + " is now available");
        }
        // if the doctor couldn't get a room under 3 seconds
        else {
          System.out.println(this + "couldn't acquire a treatement room");
          System.out.println(this + " is sending " + p + " back to the waiting room");
          p.release();
          System.out.println(this + " is now available");
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

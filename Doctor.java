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
    // TODO Auto-generated method stub
    super.run();
  }

  @Override
  public String toString() {
    return "Doctor" + id;
  }
}

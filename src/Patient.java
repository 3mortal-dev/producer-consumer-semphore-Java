package src;

public class Patient extends Thread {
  private final Integer id;
  private final WaitingRoom waitingRoom;
  private volatile Boolean isTreated = false;
  private final Semaphore treat = new Semaphore(0);

  public Patient(Integer id, WaitingRoom waitingRoom) {
    this.id = id;
    this.waitingRoom = waitingRoom;
  }

  @Override
  public void run() {
    try {
      EventLogger.patientArrived(id);
      while (true) {
        waitingRoom.produce(this);
        treat.acquire(); // wait till a doctor comes
        if (isTreated) {
          EventLogger.patientTreatmentFinished(id);
          break;
        } else {
          EventLogger.patientSentBack(id);
        }
      }
      EventLogger.patientLeft(id);
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    }
  }

  public Integer getPId() {
    return id;
  }

  public Boolean getIsTreated() {
    return isTreated;
  }

  public void setIsTreated(Boolean flag) {
    this.isTreated = flag;
  }

  // so the doctor can release if he's treated successfully
  public void release() {
    treat.release();
  }

  @Override
  public String toString() {
    return "P" + id;
  }

}

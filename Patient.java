
public class Patient extends Thread {
  private final Integer id;
  private final WaitingRoom waitingRoom;
  private volatile Boolean isTreated = false;

  public Patient(Integer id, WaitingRoom waitingRoom) {
    this.id = id;
    this.waitingRoom = waitingRoom;
  }

  @Override
  public void run() {
    // TODO Auto-generated method stub
    super.run();
  }

  public Integer getPId() {
    return id;
  }

  public Boolean getIsTreated() {
    return isTreated;
  }

  @Override
  public String toString() {
    return "P" + id;
  }

}

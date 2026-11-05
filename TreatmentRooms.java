
// those also will be represented as a Semaphore
import javax.swing.plaf.basic.BasicTreeUI.TreeHomeAction;

public class TreatmentRooms {
  private final Semaphore rooms;
  private final Integer ROOM_SIZE = 2;

  public TreatmentRooms() {
    rooms = new Semaphore(ROOM_SIZE);
  }

  public void acquire() throws InterruptedException {
    rooms.acquire();
  }

  public void release() {
    rooms.release();
  }
}

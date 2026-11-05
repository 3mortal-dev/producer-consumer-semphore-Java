public class Semaphore extends Thread {

  private int process = 0;

  protected Semaphore(int value) {
    process = value;
  }

  public synchronized void acquire() throws InterruptedException {
    process--;
    while (process < 0) {
      wait();
    }
  }

  public synchronized void signal() {
    process++;
    notify();
  }

}

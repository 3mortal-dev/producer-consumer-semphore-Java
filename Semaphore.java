public class Semaphore extends Thread {

  private int process = 0;

  protected Semaphore(int value) {
    process = value;
  }

  public synchronized boolean tryAcquire(Integer time) throws InterruptedException {
    // deadline is the current time + the period
    Long deadline = System.currentTimeMillis() + time;

    // loop till the period ends, if a you got a place then break the while loop,
    // if not, then return false
    //
    // in this particular function only we used <= not < only
    // because we can't decreament the process untill we know wheter to acquire or not
    while (process <= 0) {
      if (deadline - System.currentTimeMillis() <= 0) {
        return false;
      }

    }
    process--;
    return true;
  }

  public synchronized void acquire() throws InterruptedException {
    process--;
    while (process < 0) {
      wait();
    }
  }

  public synchronized void release() {
    process++;
    notify();
  }

}

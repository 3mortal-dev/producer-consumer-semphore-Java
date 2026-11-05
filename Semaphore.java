public class Semaphore extends Thread {

  private Integer process = 0;

  protected Semaphore(int value) {
    process = value;
  }

  public Integer getProcess() {
    return process;
  }

  public synchronized boolean tryAcquire(Integer time) throws InterruptedException {
    // deadline is the current time + the period
    Long deadline = System.currentTimeMillis() + time;

    // loop till the period ends, if a you got a place then break the while loop,
    // if not, then return false
    //
   while (process <= 0) {
      Long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) {
        return false;
      }

      wait(remaining); // so it actually waits the 3 seconds
    }
    process--;
    return true;
  }

  // That made the first patient to wait to the last doctor then infinite loops right after the first patient leave
  // Because the `full` semaphore's value becomes negative and sleeps for ever
  // public synchronized void acquire() throws InterruptedException {
  //   process--;
  //   while (process < 0) {
  //     wait();
  //   }
  // }

    public synchronized void acquire() throws InterruptedException {
    while (process < 0) {
      wait();
    }
    process--;
  }

  public synchronized void release() {
    process++;
    notify();
  }

}

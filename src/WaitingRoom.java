package src;

import java.util.LinkedList;
import java.util.Queue;

public class WaitingRoom {
  private Queue<Patient> patients = new LinkedList<>();
  private Semaphore empty;
  private Semaphore full = new Semaphore(0); // indicates the doctors

  public WaitingRoom(Integer capacity) {
    this.empty = new Semaphore(capacity);
  }
 
  public WaitingRoom(Queue<Patient> patients) {
    this.patients = patients;
  }

  // Don't sync it because it'll cause a deadlock
  // because if the room is empty and some patient tries to acquire, it'll sleep and also take the waitingRoom object to the critical section and no one will be able to access it .__.
  public void produce(Patient p) throws InterruptedException{
    // patient try to enter the room
    if (!empty.tryAcquire(0)) {  // if the room is full
      System.out.println("The waiting room is full");
      System.out.println(p + " is waiting outside for an avilable seat");
      empty.acquire();  
      }
    this.addPatient(p);
    System.out.println(p + " entered the waiting room");
    full.release(); // notify the doctors that a patient has come
  }

  // Don't sync it because it'll cause a deadlock
  public Patient consume() throws InterruptedException{
    full.acquire(); // doctor try to take a patient
    Patient p = removePatient();
    empty.release();
    return p;
  }
  
  // sync to prevent the race condition and process in a critical section
  private synchronized void addPatient(Patient p) {
    this.patients.add(p);
  }

  // sync to prevent the race condition and process in a critical section
  private synchronized Patient removePatient() {
    return this.patients.poll();
  }

}

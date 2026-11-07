package src;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Entry point for the console mode and shared simulation runner used by the GUI.
 */
public class Hospital {

  public static void main(String[] args) throws InterruptedException {
    Scanner in = new Scanner(System.in);

    System.out.println("Waiting room capacity: ");
    int capacity = in.nextInt();

    System.out.println("Number of doctors: ");
    int doctorCount = in.nextInt();

    System.out.println("Number of patients: ");
    int patientCount = in.nextInt();

    in.close();

    System.out.println();

    runSimulation(capacity, doctorCount, patientCount);
  }

  /**
   * Runs one complete simulation. Safe to call from any background thread;
   * never call this from the Swing EDT.
   */
  public static void runSimulation(int capacity, int doctorCount, int patientCount)
      throws InterruptedException {

    WaitingRoom waitingRoom = new WaitingRoom(capacity);
    TreatmentRooms treatmentRooms = new TreatmentRooms();

    List<Doctor> doctors = new ArrayList<>();
    for (int i = 1; i <= doctorCount; i++) {
      doctors.add(new Doctor(i, waitingRoom, treatmentRooms));
    }

    List<Patient> patients = new ArrayList<>();
    for (int i = 1; i <= patientCount; i++) {
      patients.add(new Patient(i, waitingRoom));
    }

    EventLogger.system("Simulation started: " + capacity + " seats, "
        + doctorCount + " doctors, " + patientCount + " patients");

    if (!EventLogger.isBroadcasting()) {
      System.out.println("Patients arriving:");
      for (int i = 1; i <= patientCount; i++) {
        System.out.print("P" + i + " ");
      }
      System.out.println();
    }

    for (Doctor d : doctors) {
      d.start();
    }

    Random random = new Random();
    for (Patient p : patients) {
      p.start();
      float speed = SimulationConfig.getSpeed();
      long gap = (long) ((100 + random.nextInt(300)) / Math.max(0.1f, speed));
      Thread.sleep(gap);
    }

    for (Patient p : patients) {
      p.join();
    }

    for (Doctor d : doctors) {
      d.interrupt();
    }

    for (Doctor d : doctors) {
      d.join();
    }

    EventLogger.system("All patients have been treated.");
    System.out.println("All patients have been treated.");
    System.out.println("Simulation completed.");
  }
}
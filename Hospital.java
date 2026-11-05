import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Entry point: reads input, creates doctors and patients, and runs the
 * simulation.
 */
// BUG: it doesn't work for sm reason
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

    System.out.println("Patients arriving:");
    for (int i = 1; i <= patientCount; i++) {
      System.out.print("P" + i + " ");
    }
    System.out.println();

    for (Doctor d : doctors) {
      d.start();
    }
    Random random = new Random();
    for (Patient p : patients) {
      p.start();
      Thread.sleep(100 + random.nextInt(300));
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

    System.out.println("All patients have been treated.");
    System.out.println("Simulation completed.");
  }
}

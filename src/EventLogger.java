package src;

import gui.events.SimulationEvent;
import gui.events.SimulationEventBroadcaster;
import gui.events.PatientEvent;
import gui.events.DoctorEvent;
import gui.events.RoomEvent;
import gui.events.SystemEvent;

/**
 * Package-private utility for logging simulation events to the GUI broadcaster.
 * Keeps domain classes clean - they just call EventLogger.log(...).
 */
public class EventLogger {
    private static volatile SimulationEventBroadcaster broadcaster;
    private static long startTime = System.currentTimeMillis();

    public static void setBroadcaster(SimulationEventBroadcaster b) {
        broadcaster = b;
        startTime = System.currentTimeMillis();
    }

    /** True when a GUI listener is attached (so console trace is suppressed). */
    static boolean isBroadcasting() {
        return broadcaster != null;
    }

    private static int timestamp() {
        return (int)(System.currentTimeMillis() - startTime);
    }

    // Patient events
    static void patientArrived(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.ARRIVED, timestamp()));
    }

    static void patientEnteredWaitingRoom(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.ENTERED_WAITING_ROOM, timestamp()));
    }

    static void patientWaitingOutside(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.WAITING_OUTSIDE, timestamp()));
    }

    static void patientTreatmentStarted(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.TREATMENT_STARTED, timestamp()));
    }

    static void patientTreatmentFinished(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.TREATMENT_FINISHED, timestamp()));
    }

    static void patientSentBack(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.SENT_BACK, timestamp()));
    }

    static void patientLeft(int patientId) {
        log(new PatientEvent(patientId, PatientEvent.PatientAction.LEFT, timestamp()));
    }

    // Doctor events
    static void doctorIdle(int doctorId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.IDLE, -1, timestamp()));
    }

    static void doctorAssignedPatient(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.ASSIGNED_PATIENT, patientId, timestamp()));
    }

    static void doctorTryingAcquireRoom(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.TRYING_ACQUIRE_ROOM, patientId, timestamp()));
    }

    static void doctorAcquiredRoom(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.ACQUIRED_ROOM, patientId, timestamp()));
    }

    static void doctorTreating(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.TREATING, patientId, timestamp()));
    }

    static void doctorReleasedRoom(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.RELEASED_ROOM, patientId, timestamp()));
    }

    static void doctorCouldNotAcquire(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.COULD_NOT_ACQUIRE, patientId, timestamp()));
    }

    static void doctorSendingBack(int doctorId, int patientId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.SENDING_BACK, patientId, timestamp()));
    }

    static void doctorAvailable(int doctorId) {
        log(new DoctorEvent(doctorId, DoctorEvent.DoctorAction.AVAILABLE, -1, timestamp()));
    }

    // Room events
    static void roomOccupied(int roomIndex, int doctorId, int patientId) {
        log(new RoomEvent(roomIndex, RoomEvent.RoomAction.OCCUPIED, doctorId, patientId, timestamp()));
    }

    static void roomReleased(int roomIndex, int doctorId, int patientId) {
        log(new RoomEvent(roomIndex, RoomEvent.RoomAction.RELEASED, doctorId, patientId, timestamp()));
    }

    // System events
    static void system(String message) {
        log(new SystemEvent(message, timestamp()));
    }

    private static void log(SimulationEvent event) {
        if (broadcaster != null) {
            broadcaster.post(event);
        } else {
            // Console mode: keep the original text-style trace.
            System.out.println(formatConsole(event));
        }
    }

    private static String formatConsole(SimulationEvent event) {
        if (event instanceof PatientEvent pe) {
            return switch (pe.action()) {
                case ARRIVED -> "P" + pe.patientId() + " arrived";
                case ENTERED_WAITING_ROOM -> "P" + pe.patientId() + " entered the waiting room";
                case WAITING_OUTSIDE -> "The waiting room is full\nP" + pe.patientId()
                        + " is waiting outside for an avilable seat";
                case TREATMENT_STARTED -> "P" + pe.patientId() + " treatment started";
                case TREATMENT_FINISHED -> "P" + pe.patientId() + " finished treatment";
                case SENT_BACK -> "P" + pe.patientId() + " was sent back";
                case LEFT -> "P" + pe.patientId() + " left";
            };
        }
        if (event instanceof DoctorEvent de) {
            String p = de.patientId() >= 0 ? " P" + de.patientId() : "";
            return switch (de.action()) {
                case IDLE -> "Doctor" + de.doctorId() + " is idle";
                case ASSIGNED_PATIENT -> "Doctor" + de.doctorId() + " is assigned to" + p;
                case TRYING_ACQUIRE_ROOM -> "Doctor" + de.doctorId() + " is trying to acquire a treatment room";
                case ACQUIRED_ROOM -> "Doctor" + de.doctorId() + " acquired a treatment room";
                case TREATING -> "Doctor" + de.doctorId() + " begins treating" + p;
                case RELEASED_ROOM -> "Doctor" + de.doctorId() + " finished treating" + p
                        + "\nDoctor" + de.doctorId() + " released the treatment room";
                case COULD_NOT_ACQUIRE -> "Doctor" + de.doctorId() + " couldn't acquire a treatment room";
                case SENDING_BACK -> "Doctor" + de.doctorId() + " is sending" + p
                        + " back to the waiting room";
                case AVAILABLE -> "Doctor" + de.doctorId() + " is now available";
            };
        }
        if (event instanceof RoomEvent re) {
            return "Room " + (re.roomIndex() + 1) + " " + re.action();
        }
        if (event instanceof SystemEvent se) {
            return se.message();
        }
        return event.toString();
    }
}
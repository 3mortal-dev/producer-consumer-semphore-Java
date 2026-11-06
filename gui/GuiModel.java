package gui;

import gui.events.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable state snapshot for GUI rendering.
 * Thread-confined to EDT - only modified via apply(event) on EDT.
 */
public final class GuiModel {
    // Waiting room state
    public final SeatState[] seats;
    public final List<Integer> outsideQueue;

    // Treatment rooms (fixed 2)
    public final RoomState room1;
    public final RoomState room2;
    public final int room1Doctor, room1Patient;
    public final int room2Doctor, room2Patient;

    // Doctors
    public final DoctorState[] doctors;

    // Event log (last 100)
    public final List<String> eventLog;

    // Control state
    public final boolean running;
    public final float speedMultiplier;
    public final int capacity;
    public final int doctorCount;
    public final int patientCount;
    public final int treatedCount;

    public GuiModel(int capacity, int doctorCount, int patientCount) {
        this.seats = new SeatState[capacity];
        for (int i = 0; i < capacity; i++) {
            this.seats[i] = SeatState.EMPTY;
        }
        this.outsideQueue = new ArrayList<>();
        this.room1 = RoomState.IDLE;
        this.room2 = RoomState.IDLE;
        this.room1Doctor = -1; this.room1Patient = -1;
        this.room2Doctor = -1; this.room2Patient = -1;
        this.doctors = new DoctorState[doctorCount];
        for (int i = 0; i < doctorCount; i++) {
            this.doctors[i] = DoctorState.IDLE;
        }
        this.eventLog = new ArrayList<>();
        this.running = false;
        this.speedMultiplier = 1.0f;
        this.capacity = capacity;
        this.doctorCount = doctorCount;
        this.patientCount = patientCount;
        this.treatedCount = 0;
    }

    private GuiModel(SeatState[] seats, List<Integer> outsideQueue,
                     RoomState room1, RoomState room2, int r1d, int r1p, int r2d, int r2p,
                     DoctorState[] doctors, List<String> eventLog,
                     boolean running, float speedMultiplier,
                     int capacity, int doctorCount, int patientCount, int treatedCount) {
        this.seats = seats;
        this.outsideQueue = outsideQueue;
        this.room1 = room1;
        this.room2 = room2;
        this.room1Doctor = r1d; this.room1Patient = r1p;
        this.room2Doctor = r2d; this.room2Patient = r2p;
        this.doctors = doctors;
        this.eventLog = eventLog;
        this.running = running;
        this.speedMultiplier = speedMultiplier;
        this.capacity = capacity;
        this.doctorCount = doctorCount;
        this.patientCount = patientCount;
        this.treatedCount = treatedCount;
    }

    private GuiModel copy(SeatState[] seats, List<Integer> outsideQueue,
                          RoomState room1, RoomState room2, int r1d, int r1p, int r2d, int r2p,
                          DoctorState[] doctors, List<String> eventLog, int treatedCount) {
        return new GuiModel(seats, outsideQueue, room1, room2, r1d, r1p, r2d, r2p,
                            doctors, eventLog, running, speedMultiplier,
                            capacity, doctorCount, patientCount, treatedCount);
    }

    public GuiModel apply(SimulationEvent event) {
        if (event instanceof PatientEvent pe) return applyPatientEvent(pe);
        if (event instanceof DoctorEvent de) return applyDoctorEvent(de);
        if (event instanceof RoomEvent re) return applyRoomEvent(re);
        if (event instanceof SystemEvent se) return applySystemEvent(se);
        return this;
    }

    private GuiModel applyPatientEvent(PatientEvent e) {
        SeatState[] newSeats = seats.clone();
        List<Integer> newOutside = new ArrayList<>(outsideQueue);
        List<String> newLog = new ArrayList<>(eventLog);
        int newTreated = treatedCount;

        switch (e.action()) {
            case ARRIVED:
                newLog.add("P" + e.patientId() + " arrived");
                break;
            case ENTERED_WAITING_ROOM:
                for (int i = 0; i < newSeats.length; i++) {
                    if (newSeats[i] == SeatState.EMPTY) {
                        newSeats[i] = SeatState.OCCUPIED;
                        break;
                    }
                }
                newOutside.remove(Integer.valueOf(e.patientId()));
                newLog.add("P" + e.patientId() + " entered waiting room");
                break;
            case WAITING_OUTSIDE:
                if (!newOutside.contains(e.patientId())) {
                    newOutside.add(e.patientId());
                }
                newLog.add("P" + e.patientId() + " waiting outside");
                break;
            case TREATMENT_STARTED:
                newLog.add("P" + e.patientId() + " treatment started");
                break;
            case TREATMENT_FINISHED:
                // Seat was already freed when the doctor consumed the patient.
                newLog.add("P" + e.patientId() + " treated");
                newTreated++;
                break;
            case SENT_BACK:
                // Seat was freed at assignment; patient will re-enter via ENTERED/WAITING_OUTSIDE.
                newLog.add("P" + e.patientId() + " sent back");
                break;
            case LEFT:
                // Seat was already freed at assignment.
                newLog.add("P" + e.patientId() + " left");
                break;
        }
        return copy(newSeats, newOutside, room1, room2, room1Doctor, room1Patient, room2Doctor, room2Patient,
                    doctors, trimLog(newLog), newTreated);
    }

    private GuiModel applyDoctorEvent(DoctorEvent e) {
        DoctorState[] newDoctors = doctors.clone();
        SeatState[] newSeats = seats.clone();
        List<String> newLog = new ArrayList<>(eventLog);

        if (e.doctorId() > 0 && e.doctorId() <= newDoctors.length) {
            int idx = e.doctorId() - 1;
            switch (e.action()) {
                case IDLE:
                    newDoctors[idx] = DoctorState.IDLE;
                    break;
                case ASSIGNED_PATIENT:
                    newDoctors[idx] = DoctorState.ASSIGNED_PATIENT;
                    // Doctor pulled the patient out of the waiting room: one seat frees up.
                    freeOneSeat(newSeats);
                    newLog.add("D" + e.doctorId() + " assigned to P" + e.patientId());
                    break;
                case TRYING_ACQUIRE_ROOM:
                    newDoctors[idx] = DoctorState.TRYING_ACQUIRE_ROOM;
                    newLog.add("D" + e.doctorId() + " trying to get room");
                    break;
                case ACQUIRED_ROOM:
                    newDoctors[idx] = DoctorState.ACQUIRED_ROOM;
                    newLog.add("D" + e.doctorId() + " got room");
                    break;
                case TREATING:
                    newDoctors[idx] = DoctorState.TREATING;
                    newLog.add("D" + e.doctorId() + " treating P" + e.patientId());
                    break;
                case RELEASED_ROOM:
                    newDoctors[idx] = DoctorState.RELEASED_ROOM;
                    newLog.add("D" + e.doctorId() + " released room");
                    break;
                case COULD_NOT_ACQUIRE:
                    newDoctors[idx] = DoctorState.COULD_NOT_ACQUIRE;
                    newLog.add("D" + e.doctorId() + " room timeout");
                    break;
                case SENDING_BACK:
                    newDoctors[idx] = DoctorState.SENDING_BACK;
                    newLog.add("D" + e.doctorId() + " sending P" + e.patientId() + " back");
                    break;
                case AVAILABLE:
                    newDoctors[idx] = DoctorState.AVAILABLE;
                    newLog.add("D" + e.doctorId() + " available");
                    break;
            }
        }
        return copy(newSeats, outsideQueue, room1, room2, room1Doctor, room1Patient, room2Doctor, room2Patient,
                    newDoctors, trimLog(newLog), treatedCount);
    }

    private GuiModel applyRoomEvent(RoomEvent e) {
        RoomState newRoom1 = room1, newRoom2 = room2;
        int r1d = room1Doctor, r1p = room1Patient, r2d = room2Doctor, r2p = room2Patient;
        List<String> newLog = new ArrayList<>(eventLog);

        boolean occupied = e.action() == RoomEvent.RoomAction.OCCUPIED;
        RoomState newState = occupied ? RoomState.OCCUPIED : RoomState.IDLE;

        if (e.roomIndex() == 0) {
            newRoom1 = newState;
            r1d = occupied ? e.doctorId() : -1;
            r1p = occupied ? e.patientId() : -1;
        } else if (e.roomIndex() == 1) {
            newRoom2 = newState;
            r2d = occupied ? e.doctorId() : -1;
            r2p = occupied ? e.patientId() : -1;
        }
        newLog.add("Room " + (e.roomIndex() + 1) + " " + e.action());
        return copy(seats, outsideQueue, newRoom1, newRoom2, r1d, r1p, r2d, r2p,
                    doctors, trimLog(newLog), treatedCount);
    }

    private GuiModel applySystemEvent(SystemEvent e) {
        List<String> newLog = new ArrayList<>(eventLog);
        newLog.add("SYS: " + e.message());
        return copy(seats, outsideQueue, room1, room2, room1Doctor, room1Patient, room2Doctor, room2Patient,
                    doctors, trimLog(newLog), treatedCount);
    }

    private static void freeOneSeat(SeatState[] seats) {
        for (int i = seats.length - 1; i >= 0; i--) {
            if (seats[i] == SeatState.OCCUPIED) {
                seats[i] = SeatState.EMPTY;
                return;
            }
        }
    }

    private static List<String> trimLog(List<String> log) {
        if (log.size() > 100) {
            return new ArrayList<>(log.subList(log.size() - 100, log.size()));
        }
        return log;
    }

    public GuiModel withRunning(boolean running) {
        return new GuiModel(seats, outsideQueue, room1, room2, room1Doctor, room1Patient, room2Doctor, room2Patient,
                            doctors, eventLog, running, speedMultiplier, capacity, doctorCount, patientCount, treatedCount);
    }

    public GuiModel withSpeed(float speed) {
        return new GuiModel(seats, outsideQueue, room1, room2, room1Doctor, room1Patient, room2Doctor, room2Patient,
                            doctors, eventLog, running, speed, capacity, doctorCount, patientCount, treatedCount);
    }

    public enum SeatState { EMPTY, OCCUPIED }
    public enum RoomState { IDLE, OCCUPIED }
    public enum DoctorState {
        IDLE, ASSIGNED_PATIENT, TRYING_ACQUIRE_ROOM, ACQUIRED_ROOM,
        TREATING, RELEASED_ROOM, COULD_NOT_ACQUIRE, SENDING_BACK, AVAILABLE
    }
}
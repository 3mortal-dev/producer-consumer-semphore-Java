package gui.game.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Authoritative, EDT-confined hospital world. Backend threads never see this
 * object; events are applied by {@link WorldReducer} after being drained from
 * GameEventBridge.
 */
public final class WorldState {
    public final int capacity;
    public final int doctorCount;
    public final int patientCount;

    public final Map<Integer, Actor> patients = new LinkedHashMap<>();
    public final Map<Integer, Actor> doctors = new LinkedHashMap<>();

    public final SlotAllocator slots;
    public final RoutePlanner router = new RoutePlanner();
    public final BedSlot[] beds;
    public final DoctorStation[] stations;

    public boolean running = true;
    public boolean complete;
    public boolean paused;
    public int arrivedCount;
    public int treatedCount;
    public int leftCount;
    public float speed = 1.0f;

    public String lastToast = "";
    public long toastUntilMs;
    public long doorOpenUntilMs;
    public String banner = "";
    public long nowMs;
    public long startedAtMs;

    public WorldState(int capacity, int doctorCount, int patientCount, float speed) {
        this.capacity = capacity;
        this.doctorCount = doctorCount;
        this.patientCount = patientCount;
        this.speed = speed;
        this.slots = new SlotAllocator(WorldLayout.generateChairs(capacity), WorldLayout.generateQueueSlots());
        this.beds = WorldLayout.generateBeds();
        this.stations = WorldLayout.generateStations(doctorCount);

        for (int i = 1; i <= doctorCount; i++) {
            Actor doctor = new Actor(i, ActorKind.DOCTOR);
            DoctorStation station = stations[Math.min(i - 1, stations.length - 1)];
            doctor.stationIndex = station.index;
            doctor.gx = station.centerX();
            doctor.gy = station.centerY();
            doctor.doctorState = DoctorState.IDLE;
            doctors.put(i, doctor);
        }
    }

    public Actor patient(int id) {
        return patients.get(id);
    }

    public Actor doctor(int id) {
        return doctors.get(id);
    }

    public Collection<Actor> patientActors() {
        return patients.values();
    }

    public Collection<Actor> doctorActors() {
        return doctors.values();
    }

    public Actor getOrCreatePatient(int id) {
        Actor actor = patients.get(id);
        if (actor == null) {
            actor = new Actor(id, ActorKind.PATIENT);
            actor.gx = WorldLayout.GATE_X;
            actor.gy = WorldLayout.GATE_Y;
            actor.stateSinceMs = nowMs;
            patients.put(id, actor);
        }
        return actor;
    }

    public Actor getOrCreatePatientAt(int id, double gx, double gy) {
        Actor actor = patients.get(id);
        if (actor == null) {
            actor = new Actor(id, ActorKind.PATIENT);
            actor.gx = gx;
            actor.gy = gy;
            actor.stateSinceMs = nowMs;
            patients.put(id, actor);
        }
        return actor;
    }

    public Actor getOrCreateDoctor(int id) {
        Actor actor = doctors.get(id);
        if (actor == null) {
            actor = new Actor(id, ActorKind.DOCTOR);
            DoctorStation station = stationFor(id);
            actor.stationIndex = station.index;
            actor.gx = station.centerX();
            actor.gy = station.centerY();
            actor.doctorState = DoctorState.IDLE;
            doctors.put(id, actor);
        }
        return actor;
    }

    public DoctorStation stationFor(int doctorId) {
        int idx = Math.floorMod(doctorId - 1, stations.length);
        return stations[idx];
    }

    public BedSlot bedForRoom(int roomIndex) {
        if (roomIndex < 0 || roomIndex >= beds.length) return null;
        return beds[roomIndex];
    }

    public BedSlot bedForPatient(int patientId) {
        for (BedSlot bed : beds) {
            if (bed.patientId == patientId) return bed;
        }
        return null;
    }

    /** True when this patient holds a pending reservation on some bed. */
    public boolean isPendingOnBed(int patientId) {
        for (BedSlot bed : beds) {
            if (bed.pendingPatientId == patientId) return true;
        }
        return false;
    }

    public BedSlot pendingBedFor(int patientId) {
        for (BedSlot bed : beds) {
            if (bed.pendingPatientId == patientId) return bed;
        }
        return null;
    }

    public void moveTo(Actor actor, int cellX, int cellY,
                       double exactX, double exactY, MoveGoal goal) {
        int startX = (int) Math.floor(actor.gx);
        int startY = (int) Math.floor(actor.gy);
        List<int[]> path = router.findPath(startX, startY, cellX, cellY);
        // Any new walk starts on the floor. The sitting/lying height is applied
        // again by arrive() when the actor reaches a chair or a bed.
        actor.z = 0;
        actor.setPath(path, exactX, exactY, goal);
        actor.stateSinceMs = nowMs;
        boolean atTarget = Math.hypot(exactX - actor.gx, exactY - actor.gy) < 0.02;
        if (path.isEmpty() && atTarget) {
            arrive(actor);
        }
    }

    public void sendToIntake(Actor patient, MoveGoal goal) {
        moveTo(patient, (int) Math.floor(WorldLayout.INTAKE_X), (int) Math.floor(WorldLayout.INTAKE_Y),
                WorldLayout.INTAKE_X, WorldLayout.INTAKE_Y, goal);
    }

    public void sendToStaging(Actor actor) {
        moveTo(actor, (int) Math.floor(WorldLayout.STAGING_X), (int) Math.floor(WorldLayout.STAGING_Y),
                WorldLayout.STAGING_X, WorldLayout.STAGING_Y, MoveGoal.STAGING);
    }

    /**
     * Idle/waiting doctors return to their own station. A single shared
     * staging tile stacked every doctor on the same spot, causing their ID
     * tags to collide whenever four doctors were between rooms.
     */
    public void sendDoctorToStaging(Actor doctor) {
        DoctorStation station = stationFor(doctor.id);
        moveTo(doctor, station.x, station.y,
                station.centerX(), station.centerY(), MoveGoal.STAGING);
    }

    public void sendDoctorToStation(Actor doctor) {
        DoctorStation station = stationFor(doctor.id);
        doctor.stationIndex = station.index;
        doctor.doctorState = DoctorState.RETURNING;
        moveTo(doctor, station.x, station.y, station.centerX(), station.centerY(), MoveGoal.STATION);
    }

    public void sendPatientToChair(Actor patient, ChairSlot chair) {
        patient.chairIndex = chair.index;
        patient.queueIndex = -1;
        patient.patientState = PatientState.TO_CHAIR;
        moveTo(patient, chair.x, chair.y, chair.centerX(), chair.centerY(), MoveGoal.CHAIR);
    }

    public void sendPatientToQueue(Actor patient, QueueSlot slot) {
        patient.queueIndex = slot.index;
        patient.patientState = PatientState.QUEUED;
        moveTo(patient, slot.x, slot.y, slot.centerX(), slot.centerY(), MoveGoal.QUEUE);
    }

    public void sendPatientToBed(Actor patient, BedSlot bed) {
        patient.roomIndex = bed.roomIndex;
        patient.patientState = PatientState.TO_BED;
        moveTo(patient, bed.approachX, bed.approachY, bed.patientX, bed.patientY, MoveGoal.BED);
    }

    public void sendDoctorToBedside(Actor doctor, BedSlot bed) {
        doctor.roomIndex = bed.roomIndex;
        doctor.bedsidePatientId = bed.patientId;
        doctor.doctorState = DoctorState.TO_BEDSIDE;
        moveTo(doctor, bed.approachX, bed.approachY, bed.doctorX, bed.doctorY, MoveGoal.BEDSIDE);
    }

    /**
     * A patient sent back after a failed room attempt re-enters the waiting
     * room. He was already inside, so he walks straight back to a chair (or
     * the outside queue when the room is full) — not via the entrance intake.
     * The following ENTERED/WAITING event is idempotent: SlotAllocator
     * returns the same chair/slot, so the walk simply replans to it.
     */
    public void sendPatientBack(Actor patient) {
        patient.leaveRequested = false;
        patient.pendingDespawn = false;
        patient.intentLying = false;
        patient.episode++;
        patient.roomIndex = -1;
        patient.patientState = PatientState.RETURNING;
        if (patient.chairIndex >= 0 || patient.queueIndex >= 0) {
            slots.releaseAll(patient.id);
            patient.chairIndex = -1;
            patient.queueIndex = -1;
        }
        // Get off the bed if necessary.
        BedSlot bed = bedForPatient(patient.id);
        if (bed != null) {
            patient.gx = bed.approachX + 0.5;
            patient.gy = bed.approachY + 0.5;
            patient.z = 0;
        }
        ChairSlot chair = slots.assignChair(patient.id);
        if (chair != null) {
            sendPatientToChair(patient, chair);
            return;
        }
        QueueSlot slot = slots.assignQueue(patient.id);
        if (slot != null) {
            sendPatientToQueue(patient, slot);
            return;
        }
        // Both chair block and queue slots are full (rare at high capacity):
        // hold at the intake until the next event frees something.
        sendToIntake(patient, MoveGoal.RETURN_INTAKE);
    }

    public void startExit(Actor patient) {
        patient.leaveRequested = true;
        patient.pendingDespawn = true;
        patient.intentLying = false;

        BedSlot vacated = bedForPatient(patient.id);
        if (vacated != null) {
            patient.gx = vacated.approachX + 0.5;
            patient.gy = vacated.approachY + 0.5;
            patient.z = 0;
            vacated.occupied = false;
            vacated.treating = false;
            vacated.patientId = -1;
            vacated.doctorId = -1;
        }

        patient.patientState = PatientState.LEAVING;
        patient.roomIndex = -1;
        slots.releaseAll(patient.id);
        patient.chairIndex = -1;
        patient.queueIndex = -1;
        // The door opens when the walker actually gets close; see update().
        moveTo(patient, (int) Math.floor(WorldLayout.GATE_X), (int) Math.floor(WorldLayout.GATE_Y),
                WorldLayout.GATE_X, WorldLayout.GATE_Y, MoveGoal.EXIT);

        if (vacated != null) {
            // The patient is off the mattress: bedside doctors may leave and
            // a pending next occupant may take the bed.
            releaseHoldingDoctors(vacated);
            promotePending(vacated);
        }
    }

    /** Doctors whose treatment is done wait here until the patient gets up. */
    private void releaseHoldingDoctors(BedSlot bed) {
        for (Actor doctor : doctors.values()) {
            if (doctor.holdAtBedside && doctor.roomIndex == bed.roomIndex) {
                doctor.holdAtBedside = false;
                doctor.bedsidePatientId = -1;
                sendDoctorToStation(doctor);
            }
        }
    }

    /** Sends a held staging pair onto a bed that has just been vacated. */
    private void promotePending(BedSlot bed) {
        int nextPatientId = bed.pendingPatientId;
        int nextDoctorId = bed.pendingDoctorId;
        bed.pendingPatientId = -1;
        bed.pendingDoctorId = -1;
        if (nextPatientId <= 0) return;

        Actor nextPatient = patient(nextPatientId);
        if (nextPatient == null || !nextPatient.visible
                || nextPatient.leaveRequested
                || nextPatient.patientState == PatientState.RECOVERING
                || nextPatient.patientState == PatientState.LEAVING
                || nextPatient.patientState == PatientState.GONE) {
            return;
        }

        bed.occupied = true;
        bed.patientId = nextPatientId;
        bed.doctorId = nextDoctorId;
        sendPatientToBed(nextPatient, bed);

        Actor nextDoctor = nextDoctorId > 0 ? doctor(nextDoctorId) : null;
        if (nextDoctor != null && nextDoctor.visible) {
            sendDoctorToBedside(nextDoctor, bed);
        }
    }

    public void update(double dtSeconds) {
        long dtMs = Math.max(0, Math.round(dtSeconds * 1000.0));
        nowMs += dtMs;
        if (dtSeconds <= 0) return;

        for (Actor patient : patients.values()) {
            if (patient.visible) advance(patient, dtSeconds);
            // STAGING is included: a patient can arrive at staging after
            // TREATMENT_FINISHED set RECOVERING, and arrive() would
            // otherwise overwrite that state and strand him there.
            if ((patient.patientState == PatientState.RECOVERING
                    || patient.patientState == PatientState.STAGING)
                    && leaveRequested(patient)
                    && patient.recoverUntilMs > 0
                    && nowMs >= patient.recoverUntilMs) {
                startExit(patient);
            }
            if (patient.patientState == PatientState.LEAVING
                    && nowMs >= doorOpenUntilMs
                    && Math.hypot(patient.gx - WorldLayout.GATE_X,
                                  patient.gy - WorldLayout.GATE_Y) < 2.2) {
                doorOpenUntilMs = nowMs + 500;
            }
        }
        for (Actor doctor : doctors.values()) {
            if (doctor.visible) advance(doctor, dtSeconds);
        }
    }

    private boolean leaveRequested(Actor patient) {
        return patient.leaveRequested;
    }

    private void advance(Actor actor, double dtSeconds) {
        actor.animTime += dtSeconds;
        if (actor.moveGoal == MoveGoal.NONE) {
            actor.walking = false;
            return;
        }

        double remaining = actor.speed * dtSeconds;
        while (remaining > 0 && actor.pathIndex < actor.path.size()) {
            int[] cell = actor.path.get(actor.pathIndex);
            double tx = cell[0] + 0.5;
            double ty = cell[1] + 0.5;
            if (moveTowards(actor, tx, ty, remaining)) {
                remaining -= Math.hypot(tx - actor.gx, ty - actor.gy);
                actor.gx = tx;
                actor.gy = ty;
                actor.pathIndex++;
            } else {
                remaining = 0;
            }
        }

        if (actor.pathIndex >= actor.path.size()) {
            if (moveTowards(actor, actor.arrivalGx, actor.arrivalGy, remaining)) {
                actor.gx = actor.arrivalGx;
                actor.gy = actor.arrivalGy;
                arrive(actor);
            }
        }
        actor.walking = actor.moveGoal != MoveGoal.NONE;
    }

    /** Moves the actor toward a point. Returns true when the point was reached. */
    private boolean moveTowards(Actor actor, double tx, double ty, double maxDistance) {
        double dx = tx - actor.gx;
        double dy = ty - actor.gy;
        double dist = Math.hypot(dx, dy);
        if (dist < 0.0001) {
            return true;
        }
        actor.facing = facingFor(dx, dy, actor.facing);
        if (dist <= maxDistance) {
            return true;
        }
        double step = maxDistance / dist;
        actor.gx += dx * step;
        actor.gy += dy * step;
        actor.walking = true;
        return false;
    }

    private Facing facingFor(double dx, double dy, Facing fallback) {
        if (Math.abs(dx) < 0.0001 && Math.abs(dy) < 0.0001) return fallback;
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? Facing.SE : Facing.NW;
        }
        return dy >= 0 ? Facing.SW : Facing.NE;
    }

    private void arrive(Actor actor) {
        MoveGoal goal = actor.moveGoal;
        actor.clearPath();
        actor.z = 0;

        if (actor.isPatient()) {
            switch (goal) {
                case INTAKE, RETURN_INTAKE -> actor.patientState = PatientState.INTAKE;
                case CHAIR -> {
                    actor.patientState = PatientState.SEATED;
                    actor.z = WorldLayout.CHAIR_SEAT_Z;
                }
                case QUEUE -> actor.patientState = PatientState.QUEUED;
                case STAGING -> actor.patientState = PatientState.STAGING;
                case BED -> {
                    actor.patientState = PatientState.LYING;
                    actor.z = WorldLayout.BED_LYING_Z;
                    actor.intentLying = true;
                }
                case EXIT -> {
                    actor.patientState = PatientState.GONE;
                    actor.visible = false;
                    leftCount++;
                    doorOpenUntilMs = nowMs + 400;
                }
                default -> { }
            }
        } else {
            switch (goal) {
                case STATION -> actor.doctorState = DoctorState.IDLE;
                case BEDSIDE -> actor.doctorState = actor.holdAtBedside
                        ? DoctorState.HOLDING_BEDSIDE
                        : actor.intentTreating ? DoctorState.TREATING
                                : DoctorState.WAITING_FOR_ROOM;
                case STAGING -> actor.doctorState = DoctorState.WAITING_FOR_ROOM;
                case RETURN_INTAKE, INTAKE -> actor.doctorState = DoctorState.IDLE;
                default -> { }
            }
        }
        actor.stateSinceMs = nowMs;
    }

    public int activePatients() {
        int count = 0;
        for (Actor patient : patients.values()) {
            if (patient.visible && patient.patientState != PatientState.GONE) count++;
        }
        return count;
    }

    public int waitingCount() {
        int count = 0;
        for (Actor patient : patients.values()) {
            if (!patient.visible) continue;
            switch (patient.patientState) {
                case ENTERING, INTAKE, QUEUED, TO_CHAIR, SEATED, RETURNING -> count++;
                default -> { }
            }
        }
        return count;
    }

    public int occupiedBeds() {
        int count = 0;
        for (BedSlot bed : beds) {
            if (bed.occupied) count++;
        }
        return count;
    }

    public void toast(String message) {
        lastToast = message == null ? "" : message;
        toastUntilMs = nowMs + 3400;
    }

    public boolean hasToast() {
        return lastToast != null && !lastToast.isBlank() && nowMs <= toastUntilMs;
    }

    public void setComplete() {
        complete = true;
        banner = "ALL PATIENTS TREATED";
        toast("Simulation complete");
    }

    public List<Actor> actorsForRendering() {
        List<Actor> list = new ArrayList<>(patients.size() + doctors.size());
        list.addAll(patients.values());
        list.addAll(doctors.values());
        return list;
    }
}

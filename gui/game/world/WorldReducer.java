package gui.game.world;

import gui.events.DoctorEvent;
import gui.events.PatientEvent;
import gui.events.RoomEvent;
import gui.events.SimulationEvent;
import gui.events.SystemEvent;

/**
 * Applies semantic backend events to the EDT-confined world. All methods here
 * run on the EDT; no backend thread can call this class.
 */
public final class WorldReducer {
    private final WorldState world;

    public WorldReducer(WorldState world) {
        this.world = world;
    }

    public void apply(SimulationEvent event) {
        if (event instanceof PatientEvent patientEvent) {
            applyPatient(patientEvent);
        } else if (event instanceof DoctorEvent doctorEvent) {
            applyDoctor(doctorEvent);
        } else if (event instanceof RoomEvent roomEvent) {
            applyRoom(roomEvent);
        } else if (event instanceof SystemEvent systemEvent) {
            applySystem(systemEvent);
        }
    }

    public WorldState world() {
        return world;
    }

    // ------------------------------------------------------------------
    // Patients
    // ------------------------------------------------------------------

    private void applyPatient(PatientEvent event) {
        Actor patient = world.getOrCreatePatient(event.patientId());
        switch (event.action()) {
            case ARRIVED -> {
                if (patient.patientState == PatientState.GONE) return;
                if (patient.patientState != PatientState.INTAKE
                        && patient.patientState != PatientState.ENTERING
                        && patient.patientState != PatientState.QUEUED
                        && patient.patientState != PatientState.TO_CHAIR) {
                    return;
                }
                patient.visible = true;
                patient.patientState = PatientState.ENTERING;
                patient.gx = WorldLayout.GATE_X;
                patient.gy = WorldLayout.GATE_Y;
                patient.z = 0;
                world.arrivedCount++;
                world.doorOpenUntilMs = world.nowMs + 600;
                world.sendToIntake(patient, MoveGoal.INTAKE);
                world.toast("P" + event.patientId() + " ARRIVED");
            }
            case WAITING_OUTSIDE -> {
                if (!patient.visible || patient.patientState == PatientState.GONE) return;
                if (patient.chairIndex >= 0) {
                    world.slots.releaseAll(patient.id);
                    patient.chairIndex = -1;
                }
                QueueSlot slot = world.slots.assignQueue(patient.id);
                patient.queueIndex = slot != null ? slot.index : -1;
                if (slot != null) {
                    world.sendPatientToQueue(patient, slot);
                } else {
                    patient.patientState = PatientState.QUEUED;
                }
                world.toast("P" + event.patientId() + " WAITING OUTSIDE");
            }
            case ENTERED_WAITING_ROOM -> {
                if (patient.patientState == PatientState.LYING
                        || patient.patientState == PatientState.TO_BED
                        || patient.patientState == PatientState.RECOVERING
                        || patient.patientState == PatientState.LEAVING
                        || patient.patientState == PatientState.GONE) {
                    return;
                }
                world.slots.releaseQueue(patient.id);
                patient.queueIndex = -1;
                ChairSlot chair = world.slots.assignChair(patient.id);
                if (chair != null) {
                    world.sendPatientToChair(patient, chair);
                    world.toast("P" + event.patientId() + " -> CHAIR " + (chair.index + 1));
                } else {
                    patient.patientState = PatientState.INTAKE;
                }
            }
            case TREATMENT_STARTED -> {
                patient.intentLying = true;
                world.toast("P" + event.patientId() + " TREATMENT STARTED");
            }
            case TREATMENT_FINISHED -> {
                world.treatedCount++;
                patient.intentLying = false;
                BedSlot bed = world.bedForPatient(patient.id);
                // A pending patient is not yet bed.patientId on any bed, so
                // bedForPatient() alone cannot see him; scan pending slots.
                boolean pending = world.isPendingOnBed(patient.id);
                if (pending) {
                    // Backend treatment finished before this patient could
                    // visually take the bed. He stays at staging; LEFT will
                    // exit him from there. Do not put him into recovery.
                    world.toast("P" + event.patientId() + " TREATED");
                    break;
                }
                patient.patientState = PatientState.RECOVERING;
                patient.stateSinceMs = world.nowMs;
                patient.recoverUntilMs = world.nowMs + 700;
                if (bed != null) {
                    bed.treating = false;
                }
                world.toast("P" + event.patientId() + " TREATED");
            }
            case SENT_BACK -> {
                // sendPatientBack needs the bed reference to lift the patient
                // off the mattress, so clear the clinical slot afterwards.
                world.sendPatientBack(patient);
                clearClinical(patient);
                world.toast("P" + event.patientId() + " SENT BACK");
            }
            case LEFT -> {
                patient.leaveRequested = true;
                BedSlot pendingBed = world.pendingBedFor(patient.id);
                if (pendingBed != null) {
                    // Never visually took the bed: cancel the reservation
                    // and exit from staging without a recovery pause.
                    pendingBed.pendingPatientId = -1;
                    pendingBed.pendingDoctorId = -1;
                    world.startExit(patient);
                    world.toast("P" + event.patientId() + " LEFT");
                    break;
                }
                if (patient.patientState == PatientState.LYING
                        || patient.patientState == PatientState.TO_BED
                        || patient.patientState == PatientState.RECOVERING
                        || patient.patientState == PatientState.STAGING
                        || patient.patientState == PatientState.TO_TREATMENT) {
                    patient.patientState = PatientState.RECOVERING;
                    long deadline = patient.recoverUntilMs == 0
                            ? world.nowMs + 500
                            : Math.min(patient.recoverUntilMs, world.nowMs + 500);
                    patient.recoverUntilMs = deadline;
                    if (world.nowMs >= deadline) {
                        world.startExit(patient);
                    }
                } else {
                    world.startExit(patient);
                }
                world.toast("P" + event.patientId() + " LEFT");
            }
        }
    }

    // ------------------------------------------------------------------
    // Doctors
    // ------------------------------------------------------------------

    private void applyDoctor(DoctorEvent event) {
        Actor doctor = world.getOrCreateDoctor(event.doctorId());
        int patientId = event.patientId();

        switch (event.action()) {
            case IDLE -> {
                doctor.doctorState = DoctorState.IDLE;
                doctor.intentTreating = false;
                world.sendDoctorToStation(doctor);
            }
            case ASSIGNED_PATIENT -> {
                doctor.doctorState = DoctorState.TO_PATIENT;
                doctor.intentTreating = false;
                // A new assignment overrides any bedside hold on the last patient.
                doctor.holdAtBedside = false;
                doctor.bedsidePatientId = -1;
                world.sendDoctorToStaging(doctor);

                Actor patient = patientId > 0 ? world.patient(patientId) : null;
                if (patient != null && patient.visible
                        && patient.patientState != PatientState.LYING
                        && patient.patientState != PatientState.TO_BED
                        && patient.patientState != PatientState.LEAVING
                        && patient.patientState != PatientState.GONE) {
                    world.slots.releaseAll(patient.id);
                    patient.chairIndex = -1;
                    patient.queueIndex = -1;
                    patient.patientState = PatientState.TO_TREATMENT;
                    patient.stateSinceMs = world.nowMs;
                    world.sendToStaging(patient);
                }
                world.toast("D" + event.doctorId() + " -> P" + patientId);
            }
            case TRYING_ACQUIRE_ROOM -> {
                doctor.doctorState = DoctorState.WAITING_FOR_ROOM;
                if (!doctor.hasPath()) {
                    world.sendDoctorToStaging(doctor);
                }
            }
            case ACQUIRED_ROOM -> {
                doctor.doctorState = DoctorState.TO_BEDSIDE;
                BedSlot bed = world.bedForRoom(doctor.roomIndex);
                if (bed != null && bed.pendingPatientId == patientId) {
                    // Bed still visually occupied: wait at staging for promotion.
                    doctor.doctorState = DoctorState.WAITING_FOR_ROOM;
                    world.sendDoctorToStaging(doctor);
                } else if (bed != null) {
                    world.sendDoctorToBedside(doctor, bed);
                } else if (!doctor.hasPath()) {
                    world.sendDoctorToStaging(doctor);
                }
            }
            case TREATING -> {
                doctor.intentTreating = true;
                doctor.doctorState = DoctorState.TO_BEDSIDE;
                BedSlot bed = world.bedForRoom(doctor.roomIndex);
                if (bed != null && bed.pendingPatientId == patientId) {
                    // Treatment started on the backend, but the bed is still
                    // visually occupied. Both actors wait at staging;
                    // promotePending() sends them in when the bed frees.
                    doctor.doctorState = DoctorState.WAITING_FOR_ROOM;
                    world.sendDoctorToStaging(doctor);
                    Actor waiting = patientId > 0 ? world.patient(patientId) : null;
                    if (waiting != null) {
                        waiting.intentLying = true;
                        waiting.roomIndex = bed.roomIndex;
                        if (waiting.patientState != PatientState.STAGING
                                && waiting.patientState != PatientState.TO_BED
                                && waiting.patientState != PatientState.LYING) {
                            world.sendToStaging(waiting);
                        }
                    }
                } else if (bed != null) {
                    bed.treating = true;
                    if (bed.patientId > 0) {
                        Actor patient = world.patient(bed.patientId);
                        if (patient != null) {
                            patient.intentLying = true;
                            if (patient.patientState != PatientState.LYING
                                    && patient.patientState != PatientState.RECOVERING
                                    && patient.patientState != PatientState.LEAVING) {
                                world.sendPatientToBed(patient, bed);
                            }
                        }
                    }
                    world.sendDoctorToBedside(doctor, bed);
                } else if (!doctor.hasPath()) {
                    world.sendDoctorToStaging(doctor);
                }
                if (patientId > 0) {
                    Actor patient = world.patient(patientId);
                    if (patient != null) patient.intentLying = true;
                }
            }
            case RELEASED_ROOM -> {
                doctor.intentTreating = false;
                BedSlot bed = world.bedForRoom(doctor.roomIndex);
                if (bed != null) bed.treating = false;
                Actor treated = patientId > 0 ? world.patient(patientId) : null;
                boolean patientOnBed = treated != null && bed != null
                        && bed.patientId == patientId
                        && (treated.patientState == PatientState.LYING
                            || treated.patientState == PatientState.TO_BED
                            || treated.patientState == PatientState.RECOVERING);
                if (patientOnBed) {
                    // Backend treatment is done, but the patient is still on
                    // the mattress. The doctor stands at the bedside until
                    // startExit() lifts the patient off (see WorldState).
                    // bed.occupied/patientId are intentionally kept: the bed
                    // is visually occupied until then.
                    doctor.holdAtBedside = true;
                    doctor.bedsidePatientId = patientId;
                    doctor.doctorState = DoctorState.HOLDING_BEDSIDE;
                    // If still walking to the bedside, keep the path; arrive()
                    // will park him in HOLDING_BEDSIDE. Otherwise he is
                    // already there and simply stays put.
                } else {
                    doctor.holdAtBedside = false;
                    doctor.doctorState = DoctorState.RETURNING;
                    world.sendDoctorToStation(doctor);
                }
                world.toast("D" + event.doctorId() + " RELEASED ROOM");
            }
            case COULD_NOT_ACQUIRE -> {
                doctor.doctorState = DoctorState.GESTURING;
                doctor.intentTreating = false;
                world.toast("D" + event.doctorId() + " NO ROOM");
            }
            case SENDING_BACK -> {
                // Doctor only gestures; the patient is routed back to seating
                // by his own SENT_BACK event (WorldState.sendPatientBack).
                doctor.doctorState = DoctorState.GESTURING;
                world.toast("D" + event.doctorId() + " SENDS P" + patientId + " BACK");
            }
            case AVAILABLE -> {
                doctor.intentTreating = false;
                if (doctor.holdAtBedside) {
                    // Stay at the bedside until the patient gets up; a later
                    // ASSIGNED event will clear the hold if he is needed.
                    break;
                }
                doctor.doctorState = DoctorState.RETURNING;
                doctor.roomIndex = -1;
                world.sendDoctorToStation(doctor);
            }
        }
    }

    // ------------------------------------------------------------------
    // Rooms
    // ------------------------------------------------------------------

    private void applyRoom(RoomEvent event) {
        BedSlot bed = world.bedForRoom(event.roomIndex());
        if (bed == null) return;

        if (event.action() == RoomEvent.RoomAction.OCCUPIED) {
            Actor patient = world.getOrCreatePatientAt(
                    event.patientId(), WorldLayout.STAGING_X, WorldLayout.STAGING_Y);
            Actor doctor = world.getOrCreateDoctor(event.doctorId());

            int previousPatient = bed.patientId;
            Actor previous = previousPatient > 0 ? world.patient(previousPatient) : null;
            boolean visuallyOccupied = previousPatient > 0
                    && previousPatient != event.patientId()
                    && previous != null && previous.visible
                    && previous.patientState != PatientState.GONE
                    && previous.patientState != PatientState.LEAVING;

            if (visuallyOccupied) {
                // The backend permit was re-issued while the previous patient
                // is still on the mattress. Hold the new pair at staging;
                // promotePending() on startExit() sends them in. The bed
                // keeps pointing at the previous patient until then.
                bed.pendingPatientId = event.patientId();
                bed.pendingDoctorId = event.doctorId();
                patient.roomIndex = event.roomIndex();
                if (patient.patientState != PatientState.LYING
                        && patient.patientState != PatientState.LEAVING
                        && patient.patientState != PatientState.GONE) {
                    world.sendToStaging(patient);
                }
                doctor.roomIndex = event.roomIndex();
                doctor.doctorState = DoctorState.WAITING_FOR_ROOM;
                world.sendDoctorToStaging(doctor);
                world.toast("P" + event.patientId() + " -> BED " + (event.roomIndex() + 1)
                        + " (WAITING)");
                return;
            }

            bed.occupied = true;
            bed.patientId = event.patientId();
            bed.doctorId = event.doctorId();
            bed.pendingPatientId = -1;
            bed.pendingDoctorId = -1;
            bed.treating = false;

            patient.roomIndex = event.roomIndex();
            if (patient.patientState != PatientState.LYING
                    && patient.patientState != PatientState.LEAVING
                    && patient.patientState != PatientState.GONE) {
                world.sendPatientToBed(patient, bed);
            }

            doctor.roomIndex = event.roomIndex();
            world.sendDoctorToBedside(doctor, bed);
            world.toast("P" + event.patientId() + " -> BED " + (event.roomIndex() + 1));
        } else {
            // Backend permit returned. Visual occupancy (bed.patientId) is
            // kept until startExit() lifts the patient off the mattress.
            bed.treating = false;
            Actor doctor = world.doctor(event.doctorId());
            if (doctor != null && doctor.doctorState != DoctorState.TREATING
                    && doctor.doctorState != DoctorState.HOLDING_BEDSIDE) {
                doctor.doctorState = DoctorState.RETURNING;
            }
            world.toast("BED " + (event.roomIndex() + 1) + " RELEASED");
        }
    }

    // ------------------------------------------------------------------
    // System
    // ------------------------------------------------------------------

    private void applySystem(SystemEvent event) {
        world.banner = event.message();
        world.toast(event.message());
        String message = event.message() == null ? "" : event.message().toLowerCase();
        if (message.contains("all patients")) {
            world.complete = true;
            world.running = false;
            world.banner = "ALL PATIENTS TREATED";
        }
    }

    private void clearClinical(Actor patient) {
        BedSlot bed = world.bedForPatient(patient.id);
        if (bed != null) {
            bed.occupied = false;
            bed.treating = false;
            bed.patientId = -1;
            bed.doctorId = -1;
        }
        for (BedSlot candidate : world.beds) {
            if (candidate.pendingPatientId == patient.id) {
                candidate.pendingPatientId = -1;
                candidate.pendingDoctorId = -1;
            }
        }
        patient.roomIndex = -1;
        patient.intentLying = false;
    }
}

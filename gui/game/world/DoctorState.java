package gui.game.world;

public enum DoctorState {
    IDLE,
    TO_PATIENT,
    WAITING_FOR_ROOM,
    TO_BEDSIDE,
    TREATING,
    /** Treatment done on the backend, but the patient is still on the bed:
     *  the doctor stands at the bedside until the patient gets up. */
    HOLDING_BEDSIDE,
    GESTURING,
    RETURNING
}

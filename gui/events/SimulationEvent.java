package gui.events;

/**
 * Base sealed interface for all simulation events.
 * Immutable data carriers for thread-safe event passing.
 */
public sealed interface SimulationEvent permits PatientEvent, DoctorEvent, RoomEvent, SystemEvent {
    int timestamp();
}
package gui.game.world;

import java.util.ArrayList;
import java.util.List;

/** A simulated patient or doctor rendered on the isometric floor. */
public final class Actor {
    public final int id;
    public final ActorKind kind;

    public PatientState patientState;
    public DoctorState doctorState;

    public double gx;
    public double gy;
    public int z;
    public Facing facing = Facing.SE;

    public List<int[]> path = List.of();
    public int pathIndex;
    public MoveGoal moveGoal = MoveGoal.NONE;
    public double arrivalGx;
    public double arrivalGy;
    public double speed;
    public double animTime;
    public boolean walking;

    public boolean visible = true;
    public boolean intentLying;
    public boolean intentTreating;
    public boolean leaveRequested;
    public boolean pendingDespawn;

    public int episode;
    public int chairIndex = -1;
    public int queueIndex = -1;
    public int roomIndex = -1;
    public int stationIndex = -1;

    public long stateSinceMs;
    public long recoverUntilMs;

    public Actor(int id, ActorKind kind) {
        this.id = id;
        this.kind = kind;
        this.speed = kind == ActorKind.PATIENT ? 5.4 : 6.4;
        if (kind == ActorKind.PATIENT) {
            patientState = PatientState.ENTERING;
        } else {
            doctorState = DoctorState.IDLE;
        }
    }

    public boolean isPatient() {
        return kind == ActorKind.PATIENT;
    }

    public boolean hasPath() {
        return pathIndex < path.size();
    }

    public void setPath(List<int[]> path, double arrivalGx, double arrivalGy, MoveGoal goal) {
        this.path = path == null ? List.of() : path;
        this.pathIndex = 0;
        this.arrivalGx = arrivalGx;
        this.arrivalGy = arrivalGy;
        this.moveGoal = goal;
        this.walking = !this.path.isEmpty();
    }

    public void clearPath() {
        this.path = List.of();
        this.pathIndex = 0;
        this.moveGoal = MoveGoal.NONE;
        this.walking = false;
    }

    public static List<int[]> emptyPath() {
        return new ArrayList<>();
    }
}

package gui.game.world;

/** Fixed data for one treatment bay. */
public final class BedSlot {
    public final int roomIndex;
    public final int bedCellX;
    public final int bedCellY;
    public final double patientX;
    public final double patientY;
    public final int approachX;
    public final int approachY;
    public final double doctorX;
    public final double doctorY;
    public final double ivX;
    public final double ivY;
    public final double monitorX;
    public final double monitorY;

    public boolean occupied;
    public boolean treating;
    public int patientId = -1;
    public int doctorId = -1;

    public BedSlot(int roomIndex,
                   int bedCellX, int bedCellY,
                   double patientX, double patientY,
                   int approachX, int approachY,
                   double doctorX, double doctorY,
                   double ivX, double ivY,
                   double monitorX, double monitorY) {
        this.roomIndex = roomIndex;
        this.bedCellX = bedCellX;
        this.bedCellY = bedCellY;
        this.patientX = patientX;
        this.patientY = patientY;
        this.approachX = approachX;
        this.approachY = approachY;
        this.doctorX = doctorX;
        this.doctorY = doctorY;
        this.ivX = ivX;
        this.ivY = ivY;
        this.monitorX = monitorX;
        this.monitorY = monitorY;
    }

    public void clearClinical() {
        occupied = false;
        treating = false;
    }
}

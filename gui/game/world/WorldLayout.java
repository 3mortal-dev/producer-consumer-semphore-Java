package gui.game.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Static floor plan for the isometric hospital. Grid coordinates are tile
 * indices; world positions use tile units with (x + 0.5, y + 0.5) as a cell
 * centre.
 */
public final class WorldLayout {
    private WorldLayout() {}

    public static final int GRID_W = 40;
    public static final int GRID_H = 18;

    public static final int TILE_W = 20;
    public static final int TILE_H = 10;
    public static final int WALL_HEIGHT = 30;

    public static final int WAITING_X0 = 2, WAITING_X1 = 19;
    public static final int WAITING_Y0 = 2, WAITING_Y1 = 15;

    public static final int CORRIDOR_X0 = 20, CORRIDOR_X1 = 24;
    public static final int CORRIDOR_Y0 = 6, CORRIDOR_Y1 = 11;

    public static final int TREAT_X0 = 25, TREAT_X1 = 38;
    public static final int TREAT_Y0 = 2, TREAT_Y1 = 15;

    public static final int APRON_X0 = 0, APRON_X1 = 1;
    public static final int APRON_Y0 = 8, APRON_Y1 = 12;

    public static final double GATE_X = 0.5, GATE_Y = 9.5;
    public static final double INTAKE_X = 3.5, INTAKE_Y = 9.5;
    public static final double STAGING_X = 22.5, STAGING_Y = 8.5;

    public static final int CHAIR_SEAT_Z = 12;
    public static final int BED_LYING_Z = 26;

    public static final int MAX_DOCTORS = 12;
    public static final int MAX_QUEUE_SLOTS = 10;

    public static boolean isFloor(int x, int y) {
        if (x >= WAITING_X0 && x <= WAITING_X1 && y >= WAITING_Y0 && y <= WAITING_Y1) return true;
        if (x >= CORRIDOR_X0 && x <= CORRIDOR_X1 && y >= CORRIDOR_Y0 && y <= CORRIDOR_Y1) return true;
        if (x >= TREAT_X0 && x <= TREAT_X1 && y >= TREAT_Y0 && y <= TREAT_Y1) return true;
        return x >= APRON_X0 && x <= APRON_X1 && y >= APRON_Y0 && y <= APRON_Y1;
    }

    public static boolean isBlocked(int x, int y) {
        // Beds
        if ((x == 27 || x == 28) && y == 7) return true;
        if ((x == 34 || x == 35) && y == 7) return true;
        // IV drips and monitors in the treatment bays
        if ((x == 26 || x == 29) && y == 6) return true;
        if ((x == 33 || x == 36) && y == 6) return true;
        // Reception desk along the waiting-room back wall
        if (x >= 14 && x <= 16 && y == 2) return true;
        // Decorative plants
        if ((x == 18 && y == 2) || (x == 2 && y == 14)) return true;
        // Vending machine against the waiting-room west wall. Keeping it at
        // x=2 leaves the chair block free even at maximum capacity.
        if (x == 2 && y == 5) return true;
        return false;
    }

    public static boolean isWalkable(int x, int y) {
        return isFloor(x, y) && !isBlocked(x, y);
    }

    public static List<ChairSlot> generateChairs(int capacity) {
        List<ChairSlot> chairs = new ArrayList<>();
        if (capacity <= 0) return chairs;

        // Two-tile pitch in both axes. The procedural chair sprite is roughly
        // one tile wide on screen; one-tile spacing made adjacent chairs and
        // seated patients overlap into an unreadable clump at close zoom.
        int columns = Math.min(9, Math.max(3, (int) Math.ceil(Math.sqrt(capacity * 1.4))));
        int rows = (int) Math.ceil(capacity / (double) columns);
        int spanX = (columns - 1) * 2 + 1;
        int spanY = (rows - 1) * 2 + 1;

        // Centre the chair block inside the waiting room. Clamp to at least one
        // tile of breathing room from the walls, and keep the first row clear
        // of the reception desk on the back wall.
        int startX = WAITING_X0 + Math.max(1,
                (WAITING_X1 - WAITING_X0 - spanX + 1) / 2);
        int startY = WAITING_Y0 + Math.max(1,
                (WAITING_Y1 - WAITING_Y0 - spanY + 1) / 2);

        int index = 0;
        for (int row = 0; row < rows && index < capacity; row++) {
            int y = startY + row * 2;
            for (int col = 0; col < columns && index < capacity; col++) {
                int x = startX + col * 2;
                chairs.add(new ChairSlot(index++, x, y));
            }
        }
        return chairs;
    }

    public static List<QueueSlot> generateQueueSlots() {
        List<QueueSlot> slots = new ArrayList<>();
        for (int i = 0; i < MAX_QUEUE_SLOTS; i++) {
            int x = APRON_X0 + (i % 2);
            int y = APRON_Y0 + (i / 2);
            slots.add(new QueueSlot(i, x, y));
        }
        return slots;
    }

    public static BedSlot[] generateBeds() {
        return new BedSlot[]{
            new BedSlot(0,
                27, 7,
                28.0, 7.5,
                28, 9,
                28.3, 9.1,
                29.5, 6.5,
                26.5, 6.5),
            new BedSlot(1,
                34, 7,
                35.0, 7.5,
                35, 9,
                35.3, 9.1,
                36.5, 6.5,
                33.5, 6.5)
        };
    }

    public static DoctorStation[] generateStations(int doctorCount) {
        int[][] base = {
            {21, 8}, {22, 8}, {23, 8}, {24, 8},
            {21, 10}, {22, 10}, {23, 10}, {24, 10},
            {27, 13}, {30, 13}, {34, 13}, {37, 13}
        };
        int count = Math.max(doctorCount, 1);
        DoctorStation[] stations = new DoctorStation[count];
        for (int i = 0; i < count; i++) {
            int[] cell;
            if (i < base.length) {
                cell = base[i];
            } else {
                int extra = i - base.length;
                cell = new int[]{26 + (extra % 12), 14};
            }
            stations[i] = new DoctorStation(i, cell[0], cell[1]);
        }
        return stations;
    }

    public static int bedCellsCount() {
        return 2;
    }
}

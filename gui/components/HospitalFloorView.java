package gui.components;

import gui.animation.Walker;
import gui.events.DoctorEvent;
import gui.events.PatientEvent;
import gui.events.RoomEvent;
import gui.events.SimulationEvent;
import gui.rendering.TileMap;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The animated hospital floor between the waiting room and the treatment rooms.
 * Draws the static tile map, then spawns short-lived walkers when the matching
 * simulation event arrives. Walkers are purely cosmetic and never touch the
 * backend threads.
 */
public class HospitalFloorView {
    private static final int MAX_WALKERS = 24;
    private static final double PATIENT_SPEED = 120.0; // px per second
    private static final double DOCTOR_SPEED = 165.0;

    private final List<Walker> walkers = new ArrayList<>();
    private final Map<Integer, Integer> lastRoomDoor = new HashMap<>();
    private long lastTickNanos = System.nanoTime();

    /** Clears all cosmetic walkers, e.g. when a new simulation starts. */
    public void reset() {
        walkers.clear();
        lastRoomDoor.clear();
        lastTickNanos = System.nanoTime();
    }

    public void update() {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        if (dt > 0.25) dt = 0.25; // avoid a huge jump after a pause
        if (dt <= 0) return;

        Iterator<Walker> it = walkers.iterator();
        while (it.hasNext()) {
            Walker w = it.next();
            w.update(dt);
            if (w.isDone()) it.remove();
        }
    }

    public void draw(Graphics2D g) {
        TileMap.drawFloor(g);
        for (Walker w : walkers) {
            w.draw(g);
        }
    }

    /** Called on the EDT for every simulation event. */
    public void onEvent(SimulationEvent event) {
        if (event instanceof RoomEvent re) {
            handleRoom(re);
        } else if (event instanceof DoctorEvent de) {
            handleDoctor(de);
        } else if (event instanceof PatientEvent pe) {
            handlePatient(pe);
        }
    }

    private void handleRoom(RoomEvent e) {
        int doorX = roomDoorX(e.roomIndex());
        if (e.action() == RoomEvent.RoomAction.OCCUPIED) {
            lastRoomDoor.put(e.patientId(), doorX);
            // Doctor walks from the middle of the corridor into the room.
            spawn(Walker.Kind.DOCTOR, List.of(
                    new int[]{TileMap.TOP_DOOR_X, TileMap.CORRIDOR_Y},
                    new int[]{doorX, TileMap.CORRIDOR_Y},
                    new int[]{doorX, TileMap.FLOOR_Y + TileMap.FLOOR_H - 16}), DOCTOR_SPEED);
        } else {
            // Doctor walks back out of the room toward the waiting room.
            spawn(Walker.Kind.DOCTOR, List.of(
                    new int[]{doorX, TileMap.FLOOR_Y + TileMap.FLOOR_H - 16},
                    new int[]{doorX, TileMap.CORRIDOR_Y},
                    new int[]{TileMap.TOP_DOOR_X, TileMap.CORRIDOR_Y},
                    new int[]{TileMap.TOP_DOOR_X, TileMap.FLOOR_Y + 14}), DOCTOR_SPEED);
        }
    }

    private void handleDoctor(DoctorEvent e) {
        switch (e.action()) {
            case ASSIGNED_PATIENT -> spawn(Walker.Kind.DOCTOR, List.of(
                    new int[]{TileMap.TOP_DOOR_X, TileMap.FLOOR_Y + 14},
                    new int[]{TileMap.TOP_DOOR_X, TileMap.CORRIDOR_Y}), DOCTOR_SPEED);
            case COULD_NOT_ACQUIRE -> spawn(Walker.Kind.DOCTOR, List.of(
                    new int[]{TileMap.TOP_DOOR_X, TileMap.CORRIDOR_Y},
                    new int[]{TileMap.TOP_DOOR_X, TileMap.FLOOR_Y + 14}), DOCTOR_SPEED);
            case SENDING_BACK -> {
                int doorX = lastRoomDoor.getOrDefault(e.patientId(), TileMap.TOP_DOOR_X);
                spawn(Walker.Kind.PATIENT, List.of(
                        new int[]{doorX, TileMap.FLOOR_Y + TileMap.FLOOR_H - 16},
                        new int[]{doorX, TileMap.CORRIDOR_Y},
                        new int[]{TileMap.TOP_DOOR_X, TileMap.CORRIDOR_Y},
                        new int[]{TileMap.TOP_DOOR_X, TileMap.FLOOR_Y + 14}), PATIENT_SPEED);
            }
            default -> { /* no walk for idle/treating states */ }
        }
    }

    private void handlePatient(PatientEvent e) {
        switch (e.action()) {
            case ARRIVED -> spawn(Walker.Kind.PATIENT, List.of(
                    new int[]{TileMap.EXIT_X, TileMap.CORRIDOR_Y},
                    new int[]{TileMap.TOP_DOOR_X, TileMap.CORRIDOR_Y},
                    new int[]{TileMap.TOP_DOOR_X, TileMap.FLOOR_Y + 14}), PATIENT_SPEED);
            case LEFT -> {
                int doorX = lastRoomDoor.getOrDefault(e.patientId(), TileMap.TOP_DOOR_X);
                spawn(Walker.Kind.PATIENT, List.of(
                        new int[]{doorX, TileMap.FLOOR_Y + TileMap.FLOOR_H - 16},
                        new int[]{doorX, TileMap.CORRIDOR_Y},
                        new int[]{TileMap.EXIT_X, TileMap.CORRIDOR_Y}), PATIENT_SPEED);
                lastRoomDoor.remove(e.patientId());
            }
            default -> { /* other patient states have no floor motion */ }
        }
    }

    private int roomDoorX(int roomIndex) {
        return roomIndex == 0 ? TileMap.ROOM1_DOOR_X : TileMap.ROOM2_DOOR_X;
    }

    private void spawn(Walker.Kind kind, List<int[]> path, double speed) {
        if (walkers.size() >= MAX_WALKERS) {
            walkers.remove(0);
        }
        walkers.add(new Walker(kind, path, speed));
    }
}
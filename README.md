# Hospital Emergency Room Simulation

CS241 Operating Systems, Assignment 1, Fall 2026

TA: Andro | Group: S7

| #   | Name          | ID       |
| --- | ------------- | -------- |
| 1   | Ammar Ayman   | 20240355 |
| 2   | NourEldin     | 20240639 |
| 3   | Youssef Saleh | 20240694 |

This is a Java simulation of a small emergency room. Patients show up, sit in a waiting room with a limited number of seats, and doctors pick them up one by one. There are only two treatment rooms, so doctors sometimes have to give up and send a patient back. All the simulation's thread coordination is done with a Semaphore class we wrote ourselves, no `java.util.concurrent`. 

> **Note:** For deep technical details on the backend concurrency, threading logic, avoiding deadlocks, and the class structure, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Download & Play

You don't need to compile any code to play! 

1. Go to the **Releases** section on the right side of this GitHub repository.
2. Download the latest `.jar` file.
3. Double-click the downloaded `.jar` file to launch the game. 

*(Alternatively, you can run it from your terminal: `java -jar Hospital.jar`)*

### Isometric Game Mode (Default)

This launches the 8-bit isometric 2.5D hospital game: an in-canvas
title screen (capacity 1-60, doctors 1-12, patients 1-99, speed 0.5x-4x), then one
shared isometric world with three camera views — `[WAITING]`, `[TREATMENT]`
(two beds with IV drips and heart monitors), and `[FULL FLOOR]` — plus a top
HUD with live `PATS / DOCS / QUEUE / BEDS / TREATED` stats, an animated
glass entrance door, and always-visible `P#`/`D#` ID tags. 

**Controls:**
* **Drag:** Pan around the hospital
* **Mouse wheel:** Zoom in and out
* **Tab / 1-3:** Switch camera views
* **Space:** Pauses (the backend keeps running)
* **T:** Toggles the one-line event strip
* **R:** Resets the camera
* **Enter:** On the completion overlay returns to the title screen

Two display settings are selectable on the title screen (chips at the bottom) and
also in-game with the keys shown in the bottom strip:

| Setting | Choices | Keys |
|---|---|---|
| **FONT** | `PIXEL` (Press Start 2P, minimum size 8) or `READABLE` (plain monospaced) | `F` |
| **PACING** | `OFF` (instant, original), `DELAY` (events spaced out), `STEP` (one event per keypress) | `V`, then `N` to step |
| **DELAY** | 0.3s / 0.7s / 1.2s / 2.0s between events in `DELAY` mode | `D` |

`DELAY` and `STEP` are purely visual. The backend simulation is untouched and keeps running underneath, pause freezes the playback clock, and completion is only shown once the queue has drained.

### Legacy 8-bit GUI Mode

If you prefer to watch the original 2D dashboard-style GUI, you can launch the game via your terminal using the legacy flag:

```bash
java -jar Hospital.jar --legacy

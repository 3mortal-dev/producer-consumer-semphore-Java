# Hospital Emergency Room Simulation

CS241 Operating Systems, Assignment 1, Fall 2026

TA: Andro | Group: S7

| #   | Name          | ID       |
| --- | ------------- | -------- |
| 1   | Ammar Ayman   | 20240355 |
| 2   | NourEldin     | 20240639 |
| 3   | Youssef Saleh | 20240694 |

This is a Java simulation of a small emergency room. Patients show up, sit in a waiting room with a limited number of seats, and doctors pick them up one by one. There are only two treatment rooms, so doctors sometimes have to give up and send a patient back. All the simulation's thread coordination is done with a Semaphore class we wrote ourselves, no `java.util.concurrent`. (The optional GUI event bus uses `java.util.concurrent` collections only to hand immutable events to Swing; no simulation logic relies on them.)

## Running it

### Console mode

```
cd src
javac *.java
java Hospital
```

It asks for three numbers: waiting room capacity, number of doctors, number of patients. Then it just prints what happens. When every patient has been treated you get "All patients have been treated." and the program exits on its own.

### Isometric game mode (default)

From the project root:

```
javac -Xlint:none -d out $(find src gui -name '*.java')
java -cp out:resources gui.Main
```

This launches the 8-bit isometric 2.5D hospital game (`gui/game/`): an in-canvas
title screen (capacity 1-60, doctors 1-12, patients 1-99, speed 0.5x-4x), then one
shared isometric world with three camera views — `[WAITING]`, `[TREATMENT]`
(two beds with IV drips and heart monitors), and `[FULL FLOOR]` — plus a top
HUD with live `PATS / DOCS / QUEUE / BEDS / TREATED` stats, an animated
glass entrance door, and always-visible `P#`/`D#` ID tags. Drag to pan, mouse wheel
to zoom, `Tab`/`1-3` switch views, `Space` pauses (the backend keeps running),
`T` toggles the one-line event strip, `R` resets the camera, and `Enter` on the
completion overlay returns to the title screen. All sprites are procedural
`Graphics2D` art; there are no external images.

Two display settings are selectable on the title screen (chips at the bottom) and
also in-game with the keys shown in the bottom strip:

| Setting | Choices | Keys |
|---|---|---|
| **FONT** | `PIXEL` (Press Start 2P, minimum size 8) or `READABLE` (plain monospaced) | `F` |
| **PACING** | `OFF` (instant, original), `DELAY` (events spaced out), `STEP` (one event per keypress) | `V`, then `N` to step |
| **DELAY** | 0.3s / 0.7s / 1.2s / 2.0s between events in `DELAY` mode | `D` |

`DELAY` and `STEP` are purely visual: `EventPlaybackQueue` sits between the event
bridge and the world model, so a burst of arrivals appears as a readable sequence
and the bottom strip shows the backlog (`DELAY 0.7S QUEUED 67`). The backend
simulation is untouched and keeps running underneath, pause freezes the playback
clock, and completion is only shown once the queue has drained.

### Bedside choreography

The backend room permit and the physical bed are deliberately different things
in the game. When treatment ends on the backend, the doctor stays at the bedside
(`HOLDING_BEDSIDE`) until the patient actually gets up and leaves the bed; only
then does he return to his station (or fetch his next patient). If the next
patient claims the bed while the previous one is still recovering on it, the new
pair waits at staging (`BedSlot.pendingPatientId`) and is promoted onto the bed
when it vacates — no sprite teleporting or eviction. Similarly, a patient sent
back after a failed room attempt walks straight back to a chair (or the outside
queue when full) instead of detouring via the entrance intake. All of this is
cosmetic, in `WorldReducer`/`WorldState`; backend concurrency is unchanged.

### Legacy 8-bit GUI mode

```
java -cp out:resources gui.Main --legacy
```

This launches the original dashboard-style GUI (`gui.HospitalGUI`). It is kept
unchanged and shares the same backend and event bus.

The font is loaded from `resources/fonts/` automatically (classpath resource, with a
filesystem fallback), so running from the project root works either way.

The GUI gives you the same three inputs plus:

| Control | Meaning |
|---|---|
| **START** | Resets the board and runs a fresh simulation |
| **PAUSE / RESUME** | Freezes the GUI only; the backend keeps running |
| **0.5x / 1x / 2x / 4x** | Real speed scaling: `Thread.sleep` values in `Doctor` and the patient arrival gap in `Hospital` are divided by this factor |

While it runs you can watch the waiting-room seats, the outside queue, both treatment
rooms, each doctor's state, live stats, and a scrolling event ticker. Between the
waiting room and the treatment rooms there is an animated **hospital floor** tile map:
patients walk in from the side entrance to the waiting room, doctors cross the
corridor into a treatment room, and treated patients walk back out. Sprites face the
direction they travel (front, left, or right) with a two-frame walk cycle; left-facing
frames are mirrored from the right-facing art. Seated patients, working doctors, and
monitors also animate with a two-frame idle loop.

A stock `.ttf` called *Press Start 2P* provides the arcade font; every sprite and tile
is drawn procedurally with `Graphics2D` (no image files).

## What is in the folder

```
README.md
TODO.md
A1.pdf
sample-output.txt
resources/
  fonts/               PressStart2P-Regular.ttf (8-bit arcade font)
src/
  Hospital.java        main(), reads input, starts everything
  Patient.java         one thread per patient
  Doctor.java          one thread per doctor
  WaitingRoom.java     the bounded buffer (seats)
  TreatmentRooms.java  the 2 rooms
  Semaphore.java       our own semaphore
  SimulationConfig.java live speed multiplier shared with the GUI
  EventLogger.java     posts simulation events to the GUI broadcaster
gui/
  Main.java            default launcher: isometric game, or --legacy dashboard GUI
  HospitalGUI.java     JFrame, bootstraps the 8-bit view (legacy GUI)
  PixelCanvas.java     60-ish FPS double-buffered renderer
  game/                isometric 2.5D game (default GUI)
    GameCanvas.java    30 FPS frame loop, input, event drain, 640x360 logical canvas
    GameSession.java   background simulation lifecycle (never touches the EDT)
    GameEventBridge.java lock-free backend -> EDT event queue
    TitleScreen.java   in-canvas start screen with editable fields
    iso/               2:1 isometric projection
    scene/             camera presets, tween, pan/zoom
    world/             EDT-confined world state, reducer, slots, routing
    render/            procedural sprite bank, world renderer, HUD renderer
  GuiModel.java        immutable snapshot of the board for rendering
  GuiEventListener.java EDT bridge (SwingUtilities.invokeLater)
  events/              immutable event records + broadcaster
  animation/           Walker: waypoint pathfinder for floor characters
  rendering/           16-color palette, procedural sprites, tile map, font
  components/          waiting room, hospital floor, treatment rooms, doctor bar, ticker, controls
```

## The 8-bit GUI layer

The GUI never touches a semaphore or blocks a simulation thread. The backend keeps
printing the same events it always did, but now also calls `EventLogger`, which posts
an immutable event to `SimulationEventBroadcaster`. That broadcaster runs on the
simulation threads, so it only enqueues and fans out; `GuiEventListener` hops to the
Swing Event Dispatch Thread with `SwingUtilities.invokeLater` and applies the event to
an immutable `GuiModel`. `PixelCanvas` then repaints from that snapshot.

This means:

- Semaphores, the waiting-room monitor, and `join()`/`interrupt()` are untouched.
- The EDT never calls `acquire()`, `wait()`, `sleep()`, or `join()`.
- Every visual update is a plain data event, so thread safety is by construction.
- Pause freezes rendering only; the simulation keeps running underneath.
- Speed changes are real: `SimulationConfig.getSpeed()` is read by backend threads,
  so treatment time, the 3-second room timeout, and arrival gaps all scale.

### The floor animation

`gui/rendering/TileMap.java` draws the static floor: a checkerboard of 16-pixel tiles,
doorways aligned with the waiting room and both treatment rooms, chevrons, signs,
plants, and a gurney. `gui/animation/Walker.java` is a tiny waypoint follower that
moves a character along a poly-line at a fixed pixels-per-second rate, choosing a
front/left/right sprite from the dominant travel axis and a two-frame walk cycle from
the distance walked.

`gui/components/HospitalFloorView.java` listens to the same simulation events as the
rest of the GUI. An arrival spawns a patient walking in from the side entrance, an
assignment spawns a doctor crossing the corridor, a room release sends the doctor back,
and a departure walks the patient out. Walkers are **purely cosmetic**: they live only
on the EDT, are capped at 24, and never read or write backend state. Starting a new
simulation clears them.

## The isometric game layer

The game reuses the exact same backend and event bus; only the presentation changes.
Simulation threads post immutable events to `SimulationEventBroadcaster`, and instead
of the legacy listener, `GameEventBridge` puts them on a lock-free queue. `GameCanvas`
drains that queue once per 33 ms frame on the EDT and applies each event through
`WorldReducer` to an EDT-confined `WorldState` (movement, seats, beds, and queues
are cosmetic mirrors of backend state — no backend concurrency or API changes).
`WorldRenderer` draws the 40x18 isometric floor plan with depth-sorted furniture and
actors, and `HudRenderer` draws the fixed 640x360 HUD (tabs, stats, speed/pause/event
controls, bottom message strip, pause/completion overlays). The framebuffer is scaled
to the window with integer nearest-neighbour scaling, so the art stays crisp at any
window size. Verified end-to-end (headless + live window): the pipeline treats and
clears 12/12 patients in the normal case, 40/40 under stress at 16x, and 60/60 in
the large case, with no NaN positions or bridge backlog.

## The idea in a few lines

The waiting room is the classic producer/consumer buffer. Patients are the producers, they put themselves in. Doctors are the consumers, they take one out. The seats are limited, so a patient has to wait outside when the room is full, and a doctor has to wait when it is empty.

The treatment rooms are a second, separate shared resource. Two rooms, any number of doctors, so they compete for them.

There is also a small third relationship that is easy to miss. After a patient sits down, its thread has nothing to do until a doctor is finished with it. So each patient owns a semaphore that starts at 0 and just sleeps on it. The doctor releases it when treatment is done, or when it gave up and is sending the patient back. Same idea as the buffer, but the "item" is just a signal.

## Class by class

### Semaphore

A counter of permits, built on `synchronized`, `wait()` and `notifyAll()`.

- `acquire()` waits while there are no permits, then takes one.
- `release()` gives a permit back and wakes up the waiting threads.
- `tryAcquire(ms)` is the one the assignment asks for. It tries to take a permit but gives up and returns `false` after the time limit.

Two details that matter:

1. The check is `while (permits <= 0) wait();` and only after that do we decrement. Waiting happens in a loop because a thread can wake up and find that someone else already took the permit.
2. In `tryAcquire` we work out a deadline once, and every time the thread wakes up we recompute how much time is left. That way the total wait never goes past the limit, even if it wakes up several times.

### WaitingRoom

Holds a queue of patients and two semaphores:

- `empty` starts at the capacity and counts free seats.
- `full` starts at 0 and counts patients waiting to be picked up.

`produce(patient)` is called by patients:

1. Try to take a free seat without blocking (`empty.tryAcquire(0)`).
2. If there is none, print that the room is full and the patient is waiting outside, then block on `empty.acquire()` until a seat opens.
3. Add the patient to the queue, print that they entered, then `full.release()` so a doctor can wake up.

`consume()` is called by doctors:

1. `full.acquire()`. This is where an idle doctor sleeps.
2. Take the patient out of the queue.
3. `empty.release()`, because a seat is free again.

Adding to and removing from the queue are two tiny `synchronized` methods, because a `LinkedList` is not safe to touch from two threads at once. Only those two lines are inside `synchronized`. The two public methods are deliberately not synchronized: if `produce` slept on `empty.acquire()` while holding the lock, no doctor could get into `consume` to free a seat, and everything would freeze.

### TreatmentRooms

Just a semaphore with the value 2, wrapped in a class so the rooms are their own resource, separate from the waiting room. A doctor calls `tryAcquire(3000)` and later `release()`.

### Patient

Prints that it arrived, then loops:

```
produce(this)         // sit down, or wait outside
treat.acquire()       // sleep until a doctor is done with me
if treated -> leave
else       -> go around again
```

A sent-back patient goes around the loop and re-enters the waiting room by itself. We did it this way on purpose. If the doctor did the re-entering and the room was full, the doctor would block, and if all the doctors did that at once nobody would be left to take patients out. Letting the patient thread do it keeps the doctors free.

### Doctor

Loops forever:

1. `consume()` a patient. Sleeps here when there is nobody.
2. Print that it is assigned and try for a room with a 3 second limit.
3. Got a room: treat for a random 2 to 5 seconds, release the room, mark the patient treated, wake the patient up.
4. No room in time: print that it could not, wake the patient up without marking it treated (so it goes back to the waiting room).

The room is released before the patient is woken, so another doctor can use it as early as possible.

### Hospital

Reads the input, creates one `WaitingRoom` and one `TreatmentRooms`, and hands the same two objects to every doctor and patient. Doctors start first, then patients one at a time with a random 0.2 to 0.8 second gap, so they arrive like real people and not all in the same millisecond.

Ending the program is a little tricky. Doctors never finish on their own, they just wait for the next patient forever. So:

1. `join()` every patient (a patient only ends once it has been treated).
2. `interrupt()` every doctor, which wakes the ones sleeping in `consume()`.
3. `join()` every doctor.

The order matters. If you join the doctors before interrupting them, the program hangs forever.

## Why it should not deadlock or corrupt anything

- The queue is only touched inside the two `synchronized` helpers.
- The number of seats and the number of waiting patients are tracked by `empty` and `full`, so the buffer can neither overflow nor be read while empty.
- We never sleep on a semaphore while holding a lock.
- A doctor never waits forever for a room, it gives up after 3 seconds.
- A doctor never waits on the waiting room when sending a patient back, the patient does that itself.

## Sample output

One real run with capacity 1, 3 doctors and 4 patients (trimmed in the middle). Your lines will come out in a different order every time, that is just how threads work.

```
P3 arrived
P3 entered the waiting room
Doctor3 is assigned to P3
Doctor3 is trying to acquire a treatment room
P4 arrived
P4 entered the waiting room
Doctor3couldn't acquire a treatment room
Doctor3 is sending P3 back to the waiting room
Doctor3 is now available
The waiting room is full
P3 is waiting outside for an avilable seat
P3 entered the waiting room
...
All patients have been treated.
Simulation completed.
```

Here Doctor1 and Doctor2 were already using both rooms, so Doctor3 waited the full 3 seconds, gave up, and P3 was sent back. P4 had taken the only seat in the meantime, so P3 had to wait outside until a doctor picked P4 up.

## Bugs we ran into

Worth writing down, because they were all small and all took a while to find.

1. **`acquire()` decremented before checking.** With `process--` first and `while (process < 0)` after, a semaphore at 0 let the first thread through. Doctors grabbed a patient that did not exist (`null`), and patients did not wait for their doctor, so they re-entered the room twice. The fix is to check `<= 0` first and only decrement after.
2. **Patients only took a seat when the room was full.** We had `empty.acquire()` inside the `if (room is full)`. Every other patient sat down without taking a seat, so the count only ever went up and the room never filled. The fix is `if (!empty.tryAcquire(0))`, which takes the seat in the normal case and only falls back to waiting when there is none.
3. **`tryAcquire` spun in a loop without `wait()`.** It held the lock the whole time, so nobody could call `release()` and it always timed out. It needs `wait(remaining)`.
4. **Joining the doctors before interrupting them.** The program printed everything and then never exited.

## Things you can change

| What | Where |
|---|---|
| Number of treatment rooms (2) | `ROOM_SIZE` in `TreatmentRooms.java` |
| How long a doctor waits for a room (3 s) | `ROOM_TIMEOUT` in `Doctor.java` |
| Treatment time (2 to 5 s) | `MIN_TREATMENT` and `MAX_TREATMENT` in `Doctor.java` |
| Gap between arrivals | the `Thread.sleep(...)` in `Hospital.java` |


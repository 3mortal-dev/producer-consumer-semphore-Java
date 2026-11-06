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

### 8-bit GUI mode

From the project root:

```
javac -d out $(find src gui -name '*.java')
java -cp out gui.HospitalGUI
```

The font is loaded from `resources/fonts/` automatically (classpath resource, with a
filesystem fallback), so running from the project root works either way.

The GUI gives you the same three inputs plus:

| Control | Meaning |
|---|---|
| **START** | Resets the board and runs a fresh simulation |
| **PAUSE / RESUME** | Freezes the GUI only; the backend keeps running |
| **0.5x / 1x / 2x / 4x** | Real speed scaling: `Thread.sleep` values in `Doctor` and the patient arrival gap in `Hospital` are divided by this factor |

While it runs you can watch the waiting-room seats, the outside queue, both treatment
rooms, each doctor's state, live stats, and a scrolling event ticker. A stock `.ttf`
called *Press Start 2P* provides the arcade font; every sprite is drawn procedurally
with `Graphics2D` (no image files).

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
  HospitalGUI.java     JFrame, bootstraps the 8-bit view
  PixelCanvas.java     60-ish FPS double-buffered renderer
  GuiModel.java        immutable snapshot of the board for rendering
  GuiEventListener.java EDT bridge (SwingUtilities.invokeLater)
  events/              immutable event records + broadcaster
  rendering/           16-color palette, procedurally drawn sprites, font
  components/          waiting room, treatment rooms, doctor bar, ticker, controls
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


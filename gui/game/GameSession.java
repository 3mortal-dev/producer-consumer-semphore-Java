package gui.game;

import gui.events.SimulationEventBroadcaster;
import src.EventLogger;
import src.Hospital;
import src.SimulationConfig;

/**
 * Owns the backend simulation lifecycle for the isometric game.
 * The simulation always runs on a background thread; this class never touches Swing.
 */
public final class GameSession {
    private final GameEventBridge bridge = new GameEventBridge();
    private volatile boolean running;
    private volatile boolean completed;
    private volatile String error;

    public boolean start(int capacity, int doctorCount, int patientCount, float speed) {
        if (running) return false;

        running = true;
        completed = false;
        error = null;
        bridge.clear();
        SimulationConfig.setSpeed(speed);

        SimulationEventBroadcaster broadcaster = SimulationEventBroadcaster.getInstance();
        broadcaster.clear();
        broadcaster.register(bridge);
        EventLogger.setBroadcaster(broadcaster);

        Thread thread = new Thread(() -> {
            try {
                Hospital.runSimulation(capacity, doctorCount, patientCount);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                error = ex.getMessage();
                ex.printStackTrace();
            } finally {
                // The bridge and its queue survive; stop future delivery only.
                SimulationEventBroadcaster.getInstance().unregister(bridge);
                completed = true;
                running = false;
            }
        }, "Simulation-Controller");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    public GameEventBridge bridge() {
        return bridge;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isCompleted() {
        return completed && !running;
    }

    public String error() {
        return error;
    }
}

package gui;

import gui.components.ControlPanel;
import gui.events.SimulationEventBroadcaster;
import src.EventLogger;
import src.Hospital;
import src.SimulationConfig;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point for the 8-bit GUI. Builds the frame, wires the event listener,
 * and launches the simulation backend on a background thread.
 */
public class HospitalGUI extends JFrame {
    private final PixelCanvas canvas;
    private final ControlPanel controlPanel;
    private volatile boolean simulating = false;

    public HospitalGUI() {
        super("8-Bit Emergency Room Simulator - CS241");

        // Placeholder initial model (real model created on START)
        canvas = new PixelCanvas(6, 4, 12);
        controlPanel = new ControlPanel(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Start a fresh simulation with the given parameters.
     * Backend runs off the EDT; events are dispatched to the GUI via the broadcaster.
     */
    public void startSimulation(int capacity, int doctorCount, int patientCount, float speed) {
        if (simulating) {
            return;
        }
        simulating = true;

        SimulationConfig.setSpeed(speed);

        GuiModel fresh = new GuiModel(capacity, doctorCount, patientCount)
                .withSpeed(speed)
                .withRunning(true);
        canvas.reset(fresh);

        SimulationEventBroadcaster broadcaster = SimulationEventBroadcaster.getInstance();
        broadcaster.clear();
        GuiEventListener listener = new GuiEventListener(canvas);
        broadcaster.register(listener);
        EventLogger.setBroadcaster(broadcaster);

        Thread simulationThread = new Thread(() -> {
            try {
                Hospital.runSimulation(capacity, doctorCount, patientCount);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                simulating = false;
                SwingUtilities.invokeLater(controlPanel::onSimulationFinished);
            }
        }, "Simulation-Controller");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    public boolean isSimulating() {
        return simulating;
    }

    public void setPaused(boolean paused) {
        canvas.setPaused(paused);
    }

    public void setSpeed(float speed) {
        SimulationConfig.setSpeed(speed);
        GuiModel current = canvas.getModel();
        if (current != null) {
            canvas.setModel(current.withSpeed(speed));
            canvas.requestRepaint();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HospitalGUI frame = new HospitalGUI();
            frame.setVisible(true);
        });
    }
}
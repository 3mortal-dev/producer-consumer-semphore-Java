package gui;

import gui.events.SimulationEvent;
import gui.events.SimulationEventListener;
import javax.swing.SwingUtilities;

/**
 * Bridges simulation events from backend threads to the EDT.
 * Applies each event to the current immutable model and requests a repaint.
 */
public final class GuiEventListener implements SimulationEventListener {
    private final PixelCanvas canvas;

    public GuiEventListener(PixelCanvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public void onEvent(SimulationEvent event) {
        // Must run on EDT for thread-safe model mutation and repaint
        SwingUtilities.invokeLater(() -> {
            GuiModel current = canvas.getModel();
            if (current == null) return;
            canvas.onSimulationEvent(event);
            GuiModel next = current.apply(event);
            canvas.setModel(next);
            canvas.requestRepaint();
        });
    }
}
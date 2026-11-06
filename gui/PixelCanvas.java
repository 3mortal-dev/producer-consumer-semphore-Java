package gui;

import gui.components.DoctorStatusBar;
import gui.components.EventLogTicker;
import gui.components.HospitalFloorView;
import gui.components.TreatmentRoomView;
import gui.components.WaitingRoomView;
import gui.events.SimulationEvent;
import gui.rendering.ColorPalette;
import gui.rendering.Font8Bit;
import gui.rendering.SpriteAtlas;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Main rendering canvas for the 8-bit hospital simulation.
 * Draws to an offscreen buffer then scales with nearest-neighbour for crisp pixels.
 */
public class PixelCanvas extends JPanel {
    private static final int BASE_WIDTH = 1024;
    private static final int BASE_HEIGHT = 768;
    private static final int RENDER_SCALE = 2;

    private GuiModel model;
    private final BufferedImage offscreenBuffer;
    private final Graphics2D offscreenGraphics;
    private final Timer animationTimer;
    private volatile boolean paused = false;

    private final WaitingRoomView waitingRoomView;
    private final HospitalFloorView hospitalFloorView;
    private final TreatmentRoomView treatmentRoomView;
    private final DoctorStatusBar doctorStatusBar;
    private final EventLogTicker eventLogTicker;

    public PixelCanvas(int capacity, int doctorCount, int patientCount) {
        this.model = new GuiModel(capacity, doctorCount, patientCount);

        setPreferredSize(new Dimension(BASE_WIDTH, BASE_HEIGHT));
        setBackground(ColorPalette.PANEL_BG);
        setDoubleBuffered(true);
        setFocusable(true);

        offscreenBuffer = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        offscreenGraphics = offscreenBuffer.createGraphics();
        offscreenGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        offscreenGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        SpriteAtlas.init();

        waitingRoomView = new WaitingRoomView(this);
        hospitalFloorView = new HospitalFloorView();
        treatmentRoomView = new TreatmentRoomView(this);
        doctorStatusBar = new DoctorStatusBar(this);
        eventLogTicker = new EventLogTicker(this);

        // ~25 FPS for smooth walk cycles while staying cheap on the EDT.
        animationTimer = new Timer(40, e -> {
            if (!paused) {
                hospitalFloorView.update();
                repaint();
            }
        });
        animationTimer.start();
    }

    public void setModel(GuiModel model) {
        this.model = model;
    }

    public GuiModel getModel() {
        return model;
    }

    public void reset(GuiModel model) {
        this.model = model;
        this.paused = false;
        hospitalFloorView.reset();
        repaint();
    }

    /** Feed a simulation event to the cosmetic floor animation layer. */
    public void onSimulationEvent(SimulationEvent event) {
        // While paused, don't spawn fresh walkers that would pile up at the
        // entrances; already-walking characters stay frozen and resume later.
        if (!paused) {
            hospitalFloorView.onEvent(event);
        }
    }

    public void requestRepaint() {
        if (!paused) repaint();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        // Repaint once so the PAUSED overlay appears immediately, then the
        // periodic timer and event-driven repaints stay quiet while paused.
        repaint();
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        offscreenGraphics.setColor(ColorPalette.PANEL_BG);
        offscreenGraphics.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);

        drawTitleBar(offscreenGraphics);
        eventLogTicker.draw(offscreenGraphics);
        waitingRoomView.draw(offscreenGraphics);
        hospitalFloorView.draw(offscreenGraphics);
        treatmentRoomView.draw(offscreenGraphics);
        drawStatsPanel(offscreenGraphics);
        doctorStatusBar.draw(offscreenGraphics);
        drawFooter(offscreenGraphics);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(offscreenBuffer, 0, 0, getWidth(), getHeight(), null);

        if (paused) {
            drawPausedOverlay(g2d);
        }
    }

    private void drawTitleBar(Graphics2D g) {
        g.setColor(ColorPalette.BLUE_DARK);
        g.fillRect(0, 0, BASE_WIDTH, 32);
        g.setColor(ColorPalette.WHITE);
        g.drawRect(0, 0, BASE_WIDTH - 1, 31);

        // Red cross icon
        g.setColor(ColorPalette.RED);
        g.fillRect(20, 10, 4, 14);
        g.fillRect(15, 15, 14, 4);

        Font8Bit.drawCentered(g, "EMERGENCY ROOM SIMULATOR", BASE_WIDTH / 2, 23, 12, ColorPalette.WHITE);
        Font8Bit.drawString(g, "CS241 OS ASSIGNMENT 1  -  8-BIT MODE", 48, 21, 7, ColorPalette.GRAY_LIGHT);
    }

    private void drawStatsPanel(Graphics2D g) {
        // Fills the empty space to the right of the two treatment rooms.
        int x = 372, y = 500, w = 636, h = 96;
        g.setColor(ColorPalette.GRAY_DARK);
        g.fillRect(x, y, w, h);
        g.setColor(ColorPalette.BLUE_LIGHT);
        g.drawRect(x, y, w - 1, h - 1);

        g.setColor(ColorPalette.BLACK);
        g.fillRect(x + 2, y + 2, 120, 14);
        Font8Bit.drawString(g, "SIM STATS", x + 6, y + 13, 7, ColorPalette.YELLOW);

        int colX = x + 12;
        int rowY = y + 30;
        Font8Bit.drawString(g, "CAPACITY: " + model.capacity, colX, rowY, 7, ColorPalette.WHITE);
        Font8Bit.drawString(g, "DOCTORS: " + model.doctorCount, colX + 160, rowY, 7, ColorPalette.WHITE);
        Font8Bit.drawString(g, "PATIENTS: " + model.patientCount, colX + 320, rowY, 7, ColorPalette.WHITE);

        rowY += 20;
        Font8Bit.drawString(g, "TREATED: " + model.treatedCount + "/" + model.patientCount, colX, rowY, 7, ColorPalette.GREEN);
        Font8Bit.drawString(g, "OUTSIDE: " + model.outsideQueue.size(), colX + 160, rowY, 7, ColorPalette.YELLOW);
        Font8Bit.drawString(g, "SPEED: " + model.speedMultiplier + "x", colX + 320, rowY, 7, ColorPalette.BLUE_LIGHT);

        rowY += 20;
        boolean bothRoomsBusy = model.room1 == GuiModel.RoomState.OCCUPIED && model.room2 == GuiModel.RoomState.OCCUPIED;
        Font8Bit.drawString(g, "ROOMS: " + (model.room1 == GuiModel.RoomState.OCCUPIED ? "1" : "-")
                + "/" + (model.room2 == GuiModel.RoomState.OCCUPIED ? "2" : "-"), colX, rowY, 7,
                bothRoomsBusy ? ColorPalette.RED : ColorPalette.GRAY_LIGHT);

        Font8Bit.drawString(g, "STATUS:", colX + 160, rowY, 7, ColorPalette.GRAY_LIGHT);
        Font8Bit.drawString(g, paused ? "PAUSED" : "RUNNING", colX + 230, rowY, 7,
                paused ? ColorPalette.YELLOW : ColorPalette.GREEN);
    }

    private void drawFooter(Graphics2D g) {
        g.setColor(ColorPalette.BLUE_DARK);
        g.fillRect(0, BASE_HEIGHT - 28, BASE_WIDTH, 28);
        g.setColor(ColorPalette.GRAY_DARK);
        g.drawLine(0, BASE_HEIGHT - 28, BASE_WIDTH, BASE_HEIGHT - 28);
        Font8Bit.drawString(g, "GROUP S7  |  AMMAR 20240355  NOURELDIN 20240639  YOUSSEF 20240694",
                16, BASE_HEIGHT - 10, 7, ColorPalette.GRAY_LIGHT);
        Font8Bit.drawRightAligned(g, "CUSTOM SEMAPHORES - NO java.util.concurrent",
                BASE_WIDTH - 16, BASE_HEIGHT - 10, 6, ColorPalette.GRAY_LIGHT);
    }

    private void drawPausedOverlay(Graphics2D g2d) {
        int w = getWidth();
        int h = getHeight();
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.fillRect(0, 0, w, h);
        Font8Bit.drawCentered(g2d, "PAUSED", w / 2, h / 2, 24, ColorPalette.YELLOW);
        Font8Bit.drawCentered(g2d, "GUI frozen - simulation continues", w / 2, h / 2 + 24, 9, ColorPalette.WHITE);
    }

    public int getBaseWidth() { return BASE_WIDTH; }
    public int getBaseHeight() { return BASE_HEIGHT; }
    public int getRenderScale() { return RENDER_SCALE; }
}
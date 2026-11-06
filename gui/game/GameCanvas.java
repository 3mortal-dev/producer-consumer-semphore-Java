package gui.game;

import gui.game.iso.IsoProjection;
import gui.game.render.HudRenderer;
import gui.game.render.WorldRenderer;
import gui.game.scene.CameraManager;
import gui.game.scene.SceneId;
import gui.game.world.WorldReducer;
import gui.game.world.WorldState;
import gui.rendering.Font8Bit;
import src.SimulationConfig;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/**
 * The whole game UI lives in one custom-painted Swing canvas. It renders at a
 * fixed 640x360 logical resolution and scales with nearest-neighbour pixels, so
 * the art stays crisp and the layout never depends on the window size.
 *
 * Backend-to-GUI communication is one-way and non-blocking: the simulation
 * posts immutable events, {@link GameEventBridge} queues them, and this canvas
 * drains the queue on the EDT each frame.
 */
public final class GameCanvas extends JPanel implements ActionListener {
    private static final int LOGICAL_W = 640;
    private static final int LOGICAL_H = 360;
    private static final int FRAME_MS = 33;

    private final BufferedImage frame;
    private final Graphics2D frameGraphics;
    private final Timer timer;

    private final WorldRenderer worldRenderer = new WorldRenderer();
    private final HudRenderer hud = new HudRenderer();

    private TitleScreen title = new TitleScreen();
    private WorldState world;
    private WorldReducer reducer;
    private CameraManager cameraManager;
    private GameSession session;
    private final EventPlaybackQueue playback = new EventPlaybackQueue();

    private boolean showTitle = true;
    private boolean paused;
    private boolean showToasts = true;
    private boolean dragging;
    private int lastMouseX;
    private int lastMouseY;
    private long lastTickNanos = System.nanoTime();
    private long elapsedMs;

    public GameCanvas() {
        setPreferredSize(new Dimension(LOGICAL_W * 2, LOGICAL_H * 2));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setDoubleBuffered(true);

        frame = new BufferedImage(LOGICAL_W, LOGICAL_H, BufferedImage.TYPE_INT_ARGB);
        frameGraphics = frame.createGraphics();
        frameGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        frameGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        frameGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        installInput();

        timer = new Timer(FRAME_MS, this);
        timer.setCoalesce(true);
        timer.start();
    }

    // ------------------------------------------------------------------
    // Frame loop
    // ------------------------------------------------------------------

    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        if (dt > 0.12) dt = 0.12; // avoid huge jumps after a window stall
        elapsedMs += Math.round(dt * 1000.0);

        if (!showTitle && world != null) {
            if (session != null) {
                // Backend -> bridge -> playback queue -> reducer. The playback
                // queue paces events so bursts read as a sequence; in OFF mode
                // it is a pass-through. The clock is world time, so pause
                // freezes playback too.
                session.bridge().drain(event -> playback.enqueue(event, world.nowMs));
                playback.release(world.nowMs, reducer::apply);
            }

            if (session != null && session.error() != null
                    && !world.complete) {
                world.toast("SIMULATION ERROR: " + session.error());
            } else if (session != null && session.isCompleted()
                    && playback.isEmpty()
                    && session.error() == null && !world.complete) {
                // Fallback in case the final system event was dropped. Gated on
                // an empty playback queue so completion never overtakes events.
                world.complete = true;
                world.running = false;
                world.banner = "ALL PATIENTS TREATED";
            }

            if (!paused) {
                world.update(dt);
            }
            if (cameraManager != null) {
                // Camera tweening keeps working while the world is paused, so
                // switching tabs never feels stuck.
                cameraManager.update(dt);
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = frameGraphics;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Readable type gets smooth text; the pixel face stays hard-edged.
        boolean readable = Font8Bit.isReadable();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                readable ? RenderingHints.VALUE_ANTIALIAS_ON
                        : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                readable ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                        : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        if (showTitle || world == null) {
            title.draw(g, elapsedMs);
        } else {
            worldRenderer.render(g, world, cameraManager.camera(), cameraManager.activeScene());
            hud.draw(g, world, cameraManager.activeScene(), world.speed, paused, showToasts,
                    pacingText());
            hud.drawOverlays(g, world, paused);
        }

        Graphics2D out = (Graphics2D) graphics.create();
        out.setColor(Color.BLACK);
        out.fillRect(0, 0, getWidth(), getHeight());
        int scale = Math.max(1, Math.min(getWidth() / LOGICAL_W, getHeight() / LOGICAL_H));
        int drawW = LOGICAL_W * scale;
        int drawH = LOGICAL_H * scale;
        int offsetX = (getWidth() - drawW) / 2;
        int offsetY = (getHeight() - drawH) / 2;
        out.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        out.drawImage(frame, offsetX, offsetY, drawW, drawH, null);
        out.dispose();
    }

    // ------------------------------------------------------------------
    // Session lifecycle
    // ------------------------------------------------------------------

    private void startFromTitle() {
        TitleScreen.StartConfig config = title.consumeStart();
        showToasts = title.toastsEnabled();
        hud.setFontMode(config.fontMode());
        playback.setMode(config.pacingMode());
        playback.setDelayIndex(config.delayIndex());
        playback.clear();
        startSimulation(config.capacity(), config.doctors(), config.patients(), config.speed());
    }

    private String pacingText() {
        if (playback.mode() == EventPlaybackQueue.Mode.OFF) return "";
        int queued = playback.backlog();
        return switch (playback.mode()) {
            case STEP -> "STEP: PRESS N (QUEUED " + queued + ")";
            case DELAY -> "DELAY " + playback.delayLabel() + " QUEUED " + queued;
            case OFF -> "";
        };
    }

    public void startSimulation(int capacity, int doctors, int patients, float speed) {
        if (session != null && session.isRunning()) return;

        playback.clear();
        world = new WorldState(capacity, doctors, patients, speed);
        reducer = new WorldReducer(world);
        cameraManager = new CameraManager();
        paused = false;
        world.paused = false;
        world.speed = speed;
        SimulationConfig.setSpeed(speed);

        session = new GameSession();
        if (!session.start(capacity, doctors, patients, speed)) {
            showTitle = true;
            title.setToastsEnabled(showToasts);
            return;
        }

        showTitle = false;
        dragging = false;
        lastTickNanos = System.nanoTime();
        requestFocusInWindow();
        repaint();
    }

    private void returnToTitle() {
        if (session != null && session.isRunning()) return;
        boolean toasts = showToasts;
        Font8Bit.FontMode font = Font8Bit.getMode();
        EventPlaybackQueue.Mode pacing = playback.mode();
        int delay = playback.delayIndex();
        title = new TitleScreen();
        title.setToastsEnabled(toasts);
        title.setFontMode(font);
        title.setPacingMode(pacing);
        title.setDelayIndex(delay);
        showTitle = true;
        world = null;
        reducer = null;
        cameraManager = null;
        session = null;
        paused = false;
        dragging = false;
        repaint();
    }

    private void togglePause() {
        if (world == null) return;
        paused = !paused;
        world.paused = paused;
    }

    private void toggleToasts() {
        showToasts = !showToasts;
    }

    private void setSpeed(float speed) {
        SimulationConfig.setSpeed(speed);
        if (world != null) world.speed = speed;
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    private void installInput() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                int x = toLogicalX(e.getX());
                int y = toLogicalY(e.getY());
                onMousePressed(e, x, y);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(toLogicalX(e.getX()), toLogicalY(e.getY()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (showTitle || world == null) return;
                int x = toLogicalX(e.getX());
                int y = toLogicalY(e.getY());
                if (dragging) {
                    cameraManager.pan(x - lastMouseX, y - lastMouseY);
                }
                lastMouseX = x;
                lastMouseY = y;
            }
        });

        addMouseWheelListener(this::onWheel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                onKeyPressed(e);
            }
        });
    }

    private void onMousePressed(MouseEvent e, int x, int y) {
        if (showTitle || world == null) {
            title.mousePressed(x, y);
            if (title.startRequested()) {
                startFromTitle();
            }
            return;
        }

        SceneId tab = hud.sceneAt(x, y);
        if (tab != null) {
            cameraManager.setScene(tab);
            return;
        }
        int speedIndex = hud.speedAt(x, y);
        if (speedIndex >= 0) {
            setSpeed(HudRenderer.SPEEDS[speedIndex]);
            return;
        }
        if (hud.pauseAt(x, y)) {
            togglePause();
            return;
        }
        if (hud.eventsAt(x, y)) {
            toggleToasts();
            return;
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            cameraManager.resetView();
            return;
        }

        if (y >= IsoProjection.VIEW_Y && y < HudRenderer.BOTTOM_Y) {
            dragging = true;
            lastMouseX = x;
            lastMouseY = y;
        }
    }

    private void onWheel(MouseWheelEvent e) {
        if (showTitle || world == null) return;
        int x = toLogicalX(e.getX());
        int y = toLogicalY(e.getY());
        if (y < IsoProjection.VIEW_Y || y >= HudRenderer.BOTTOM_Y) return;
        double factor = Math.pow(1.12, -e.getPreciseWheelRotation());
        cameraManager.zoomBy(factor);
        updateHover(x, y);
    }

    private void onKeyPressed(KeyEvent e) {
        if (showTitle || world == null) {
            title.keyPressed(e.getKeyCode(), e.getKeyChar());
            showToasts = title.toastsEnabled();
            if (title.startRequested()) {
                startFromTitle();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_TAB -> {
                SceneId[] scenes = SceneId.values();
                int next = cameraManager.activeScene().ordinal()
                        + (e.isShiftDown() ? -1 : 1);
                next = Math.floorMod(next, scenes.length);
                cameraManager.setScene(scenes[next]);
            }
            case KeyEvent.VK_1 -> cameraManager.setScene(SceneId.WAITING_ROOM);
            case KeyEvent.VK_2 -> cameraManager.setScene(SceneId.TREATMENT_WING);
            case KeyEvent.VK_3 -> cameraManager.setScene(SceneId.FULL_FLOOR);
            case KeyEvent.VK_SPACE, KeyEvent.VK_P -> togglePause();
            case KeyEvent.VK_T -> toggleToasts();
            case KeyEvent.VK_F -> hud.setFontMode(Font8Bit.getMode() == Font8Bit.FontMode.PIXEL
                    ? Font8Bit.FontMode.READABLE : Font8Bit.FontMode.PIXEL);
            case KeyEvent.VK_V -> playback.setMode(switch (playback.mode()) {
                case OFF -> EventPlaybackQueue.Mode.DELAY;
                case DELAY -> EventPlaybackQueue.Mode.STEP;
                case STEP -> EventPlaybackQueue.Mode.OFF;
            });
            case KeyEvent.VK_D -> playback.cycleDelay();
            case KeyEvent.VK_N, KeyEvent.VK_PERIOD -> {
                if (playback.mode() == EventPlaybackQueue.Mode.STEP) {
                    playback.step(reducer::apply);
                }
            }
            case KeyEvent.VK_R -> cameraManager.resetView();
            case KeyEvent.VK_ENTER -> {
                if (world.complete) returnToTitle();
            }
            default -> {
                // No other gameplay keys yet.
            }
        }
    }

    private void updateHover(int x, int y) {
        if (showTitle || world == null) {
            title.mouseMoved(x, y);
            setCursor(Cursor.getPredefinedCursor(
                    title.isInteractive(x, y) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            return;
        }
        hud.setHover(x, y);
        if (hud.isInteractive(x, y)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (y >= IsoProjection.VIEW_Y && y < HudRenderer.BOTTOM_Y) {
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        } else {
            hud.clearHover();
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private int toLogicalX(int panelX) {
        int scale = Math.max(1, Math.min(getWidth() / LOGICAL_W, getHeight() / LOGICAL_H));
        int offsetX = (getWidth() - LOGICAL_W * scale) / 2;
        return (panelX - offsetX) / scale;
    }

    private int toLogicalY(int panelY) {
        int scale = Math.max(1, Math.min(getWidth() / LOGICAL_W, getHeight() / LOGICAL_H));
        int offsetY = (getHeight() - LOGICAL_H * scale) / 2;
        return (panelY - offsetY) / scale;
    }

    // ------------------------------------------------------------------
    // Test/debug helpers
    // ------------------------------------------------------------------

    /** Renders the current screen to an image without going through the window. */
    public BufferedImage renderNow() {
        BufferedImage copy = new BufferedImage(LOGICAL_W, LOGICAL_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(frame, 0, 0, null);
        g.dispose();
        return copy;
    }

    public WorldState world() {
        return world;
    }

    public boolean isShowingTitle() {
        return showTitle;
    }

    public TitleScreen titleScreen() {
        return title;
    }
}

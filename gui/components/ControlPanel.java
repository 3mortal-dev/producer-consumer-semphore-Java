package gui.components;

import gui.HospitalGUI;
import gui.rendering.ColorPalette;
import gui.rendering.Font8Bit;
import src.SimulationConfig;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Arcade-style control panel: simulation inputs, start, pause, and speed controls.
 * Real Swing widgets styled to fit the 8-bit theme.
 */
public class ControlPanel extends JPanel {
    private final HospitalGUI gui;

    private final JTextField capacityField = new JTextField("6", 3);
    private final JTextField doctorsField = new JTextField("4", 3);
    private final JTextField patientsField = new JTextField("12", 3);
    private final JButton startButton = new JButton("START");
    private final JButton pauseButton = new JButton("PAUSE");
    private final JLabel statusLabel = new JLabel("READY");
    private final JButton[] speedButtons = new JButton[4];
    private static final float[] SPEEDS = {0.5f, 1.0f, 2.0f, 4.0f};

    private float speed = 1.0f;
    private boolean paused = false;

    public ControlPanel(HospitalGUI gui) {
        this.gui = gui;
        setBackground(ColorPalette.BLUE_DARK);
        setPreferredSize(new Dimension(1024, 56));
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ColorPalette.BLACK, 2),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));

        add(makeLabel("CAPACITY"));
        add(styleField(capacityField));
        add(makeLabel("DOCTORS"));
        add(styleField(doctorsField));
        add(makeLabel("PATIENTS"));
        add(styleField(patientsField));

        add(Box.createHorizontalStrut(6));
        styleButton(startButton, ColorPalette.GREEN, ColorPalette.BLACK);
        add(startButton);

        styleButton(pauseButton, ColorPalette.YELLOW, ColorPalette.BLACK);
        add(pauseButton);

        add(makeLabel("SPEED"));
        String[] labels = {"0.5x", "1x", "2x", "4x"};
        for (int i = 0; i < speedButtons.length; i++) {
            final float s = SPEEDS[i];
            JButton b = new JButton(labels[i]);
            styleButton(b, ColorPalette.BLUE_LIGHT, ColorPalette.WHITE);
            b.addActionListener(e -> setSpeed(s));
            speedButtons[i] = b;
            add(b);
        }

        add(Box.createHorizontalStrut(8));
        statusLabel.setFont(Font8Bit.get(8));
        statusLabel.setForeground(ColorPalette.GRAY_LIGHT);
        add(statusLabel);

        startButton.addActionListener(e -> onStart());
        pauseButton.addActionListener(e -> onPause());
        pauseButton.setEnabled(false);

        highlightSpeed(1.0f);
    }

    /** Called on the EDT when the backend simulation finishes. */
    public void onSimulationFinished() {
        paused = false;
        pauseButton.setEnabled(false);
        pauseButton.setText("PAUSE");
        startButton.setEnabled(true);
        setStatus("COMPLETED", ColorPalette.GREEN);
    }

    private void onStart() {
        if (gui.isSimulating()) {
            setStatus("ALREADY RUNNING", ColorPalette.YELLOW);
            return;
        }
        Integer capacity = parse(capacityField);
        Integer doctors = parse(doctorsField);
        Integer patients = parse(patientsField);
        if (capacity == null || doctors == null || patients == null) {
            setStatus("INVALID INPUT", ColorPalette.RED);
            return;
        }
        if (capacity <= 0 || doctors <= 0 || patients <= 0) {
            setStatus("VALUES MUST BE > 0", ColorPalette.RED);
            return;
        }
        if (capacity > 60) {
            setStatus("MAX CAPACITY 60", ColorPalette.RED);
            return;
        }

        paused = false;
        pauseButton.setEnabled(true);
        pauseButton.setText("PAUSE");
        startButton.setEnabled(false);
        setStatus("RUNNING", ColorPalette.GREEN);
        gui.startSimulation(capacity, doctors, patients, speed);
    }

    private void onPause() {
        paused = !paused;
        gui.setPaused(paused);
        pauseButton.setText(paused ? "RESUME" : "PAUSE");
        setStatus(paused ? "PAUSED" : "RUNNING",
                  paused ? ColorPalette.YELLOW : ColorPalette.GREEN);
    }

    private void setSpeed(float s) {
        this.speed = s;
        SimulationConfig.setSpeed(s);
        gui.setSpeed(s);
        highlightSpeed(s);
    }

    private void highlightSpeed(float s) {
        for (int i = 0; i < speedButtons.length; i++) {
            boolean active = Math.abs(SPEEDS[i] - s) < 0.001f;
            speedButtons[i].setBackground(active ? ColorPalette.YELLOW : ColorPalette.GRAY_DARK);
            speedButtons[i].setForeground(active ? ColorPalette.BLACK : ColorPalette.WHITE);
        }
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private Integer parse(JTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Font8Bit.get(7));
        l.setForeground(ColorPalette.WHITE);
        return l;
    }

    private JTextField styleField(JTextField field) {
        field.setFont(Font8Bit.get(8));
        field.setBackground(ColorPalette.BLACK);
        field.setForeground(ColorPalette.WHITE);
        field.setCaretColor(ColorPalette.WHITE);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBorder(new LineBorder(ColorPalette.GRAY_LIGHT, 1));
        field.setPreferredSize(new Dimension(46, 24));
        return field;
    }

    private void styleButton(JButton button, Color bg, Color fg) {
        button.setFont(Font8Bit.get(7));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(ColorPalette.BLACK, 2),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }
}
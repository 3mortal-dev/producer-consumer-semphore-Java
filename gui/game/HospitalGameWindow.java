package gui.game;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Frame for the isometric game. All simulation work stays on a background
 * thread; this class only hosts the EDT-confined canvas.
 */
public final class HospitalGameWindow extends JFrame {
    private final GameCanvas canvas;

    public HospitalGameWindow() {
        super("8-Bit Emergency Room - Isometric Edition");
        canvas = new GameCanvas();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        setIconImage(makeIcon());
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            HospitalGameWindow window = new HospitalGameWindow();
            window.setVisible(true);
            window.canvas.requestFocusInWindow();
        });
    }

    public static void main(String[] args) {
        launch();
    }

    public GameCanvas canvas() {
        return canvas;
    }

    private static BufferedImage makeIcon() {
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setColor(new Color(0x10, 0x14, 0x20));
        g.fillRect(0, 0, 32, 32);
        g.setColor(new Color(0xE8, 0x38, 0x30));
        g.fillRect(12, 5, 8, 22);
        g.fillRect(5, 12, 22, 8);
        g.setColor(new Color(0xF8, 0xF8, 0xFF));
        g.fillRect(14, 7, 2, 18);
        g.fillRect(7, 14, 18, 2);
        g.dispose();
        return icon;
    }
}

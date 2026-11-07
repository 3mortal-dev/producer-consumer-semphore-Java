package gui;

import gui.game.HospitalGameWindow;

/**
 * Default launcher for the project.
 *
 * <pre>
 *   java -cp out:resources gui.Main            # isometric game
 *   java -cp out:resources gui.Main --legacy   # original dashboard GUI
 * </pre>
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        for (String arg : args) {
            if ("--legacy".equalsIgnoreCase(arg)) {
                HospitalGUI.main(new String[0]);
                return;
            }
            if ("--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg)) {
                printUsage();
                return;
            }
        }
        HospitalGameWindow.launch();
    }

    private static void printUsage() {
        System.out.println("8-Bit Emergency Room Simulator");
        System.out.println();
        System.out.println("  java -cp out:resources gui.Main            launch the isometric game");
        System.out.println("  java -cp out:resources gui.Main --legacy   launch the original dashboard GUI");
        System.out.println("  java -cp out:resources src.Hospital        console mode");
    }
}

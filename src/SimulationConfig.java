package src;

/**
 * Shared, thread-safe configuration for the running simulation.
 * Speed multiplier is read by backend threads each cycle so it can change live.
 */
public final class SimulationConfig {
    private static volatile float speed = 1.0f;

    private SimulationConfig() {}

    public static void setSpeed(float s) {
        speed = Math.max(0.1f, Math.min(16.0f, s));
    }

    public static float getSpeed() {
        return speed;
    }
}
package gui.animation;

import gui.rendering.SpriteAtlas;
import gui.rendering.SpriteRenderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A transient 8-bit character that walks along a poly-line of waypoints on the
 * hospital floor. Waypoints are centre points in logical pixels.
 */
public final class Walker {
    public enum Kind { PATIENT, DOCTOR }

    private enum Dir { LEFT, RIGHT, FRONT }

    private final Kind kind;
    private final List<int[]> points;
    private final double speed;
    private int segment = 0;
    private double x;
    private double y;
    private double walked = 0;
    private Dir dir = Dir.FRONT;
    private boolean done = false;

    public Walker(Kind kind, List<int[]> points, double pixelsPerSecond) {
        this.kind = kind;
        this.points = points;
        this.speed = pixelsPerSecond;
        if (!points.isEmpty()) {
            x = points.get(0)[0];
            y = points.get(0)[1];
        }
        if (points.size() < 2) {
            done = true;
        }
    }

    public boolean isDone() {
        return done;
    }

    public void update(double dtSeconds) {
        if (done) return;

        int[] target = points.get(segment + 1);
        double dx = target[0] - x;
        double dy = target[1] - y;
        double dist = Math.hypot(dx, dy);
        double step = speed * dtSeconds;

        // Face the dominant axis of travel.
        if (Math.abs(dx) > Math.abs(dy)) {
            dir = dx >= 0 ? Dir.RIGHT : Dir.LEFT;
        } else {
            dir = Dir.FRONT;
        }

        if (dist <= step || dist == 0) {
            x = target[0];
            y = target[1];
            walked += dist;
            segment++;
            if (segment >= points.size() - 1) {
                done = true;
            }
        } else {
            x += dx / dist * step;
            y += dy / dist * step;
            walked += step;
        }
    }

    public void draw(Graphics2D g) {
        int frame = ((int) (walked / 7)) % 2;
        String base = (kind == Kind.PATIENT ? "patient_walk_" : "doctor_walk_");
        String dirKey = switch (dir) {
            case LEFT -> "left";
            case RIGHT -> "right";
            case FRONT -> "front";
        };
        BufferedImage sprite = SpriteAtlas.get(base + dirKey + "_f" + frame);
        SpriteRenderer.draw(g, sprite, (int) Math.round(x) - 8, (int) Math.round(y) - 8);
    }
}
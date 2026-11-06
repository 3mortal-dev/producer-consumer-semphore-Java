package gui.game.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Small A* pathfinder over the 40x18 walkable hospital grid. Paths are lists of
 * tile cells from the cell after the start through the target cell.
 */
public final class RoutePlanner {

    private static final int[][] DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public List<int[]> findPath(int startX, int startY, int targetX, int targetY) {
        if (!WorldLayout.isFloor(targetX, targetY)) {
            return Collections.emptyList();
        }

        int sx = startX;
        int sy = startY;
        if (!WorldLayout.isWalkable(sx, sy)) {
            int[] nearest = nearestWalkable(sx, sy);
            if (nearest == null) return Collections.emptyList();
            sx = nearest[0];
            sy = nearest[1];
        }

        if (sx == targetX && sy == targetY) {
            return Collections.emptyList();
        }

        Map<Integer, Integer> cameFrom = new HashMap<>();
        Map<Integer, Double> gScore = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        int startKey = key(sx, sy);
        gScore.put(startKey, 0.0);
        open.add(new Node(sx, sy, 0.0, heuristic(sx, sy, targetX, targetY)));

        boolean found = false;
        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.x == targetX && current.y == targetY) {
                found = true;
                break;
            }
            double currentG = gScore.getOrDefault(key(current.x, current.y), Double.MAX_VALUE);
            if (current.g > currentG) continue;

            for (int[] d : DIRS) {
                int nx = current.x + d[0];
                int ny = current.y + d[1];
                if (!WorldLayout.isFloor(nx, ny)) continue;

                boolean isTarget = nx == targetX && ny == targetY;
                if (!isTarget && !WorldLayout.isWalkable(nx, ny)) continue;

                if (d[0] != 0 && d[1] != 0) {
                    // Prevent cutting through diagonal wall corners.
                    if (!WorldLayout.isWalkable(current.x + d[0], current.y)
                            || !WorldLayout.isWalkable(current.x, current.y + d[1])) {
                        continue;
                    }
                }

                double step = (d[0] != 0 && d[1] != 0) ? 1.41421356 : 1.0;
                double tentative = currentG + step;
                int nk = key(nx, ny);
                if (tentative < gScore.getOrDefault(nk, Double.MAX_VALUE)) {
                    gScore.put(nk, tentative);
                    cameFrom.put(nk, key(current.x, current.y));
                    open.add(new Node(nx, ny, tentative, tentative + heuristic(nx, ny, targetX, targetY)));
                }
            }
        }

        if (!found) {
            // Fallback: walk straight to the target cell's centre. Cosmetic
            // movement must never deadlock because a path was unavailable.
            return List.of(new int[]{targetX, targetY});
        }

        List<int[]> path = new ArrayList<>();
        int cx = targetX;
        int cy = targetY;
        while (cx != sx || cy != sy) {
            path.add(new int[]{cx, cy});
            Integer prev = cameFrom.get(key(cx, cy));
            if (prev == null) break;
            cx = prev % WorldLayout.GRID_W;
            cy = prev / WorldLayout.GRID_W;
        }
        Collections.reverse(path);
        return path;
    }

    public int[] nearestWalkable(int x, int y) {
        if (WorldLayout.isWalkable(x, y)) return new int[]{x, y};
        for (int radius = 1; radius <= 6; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) continue;
                    int nx = x + dx;
                    int ny = y + dy;
                    if (WorldLayout.isWalkable(nx, ny)) return new int[]{nx, ny};
                }
            }
        }
        return null;
    }

    private static double heuristic(int x, int y, int tx, int ty) {
        int dx = Math.abs(x - tx);
        int dy = Math.abs(y - ty);
        return (dx + dy) + (1.41421356 - 2) * Math.min(dx, dy);
    }

    private static int key(int x, int y) {
        return y * WorldLayout.GRID_W + x;
    }

    private record Node(int x, int y, double g, double f) implements Comparable<Node> {
        @Override
        public int compareTo(Node other) {
            return Double.compare(f, other.f);
        }
    }
}

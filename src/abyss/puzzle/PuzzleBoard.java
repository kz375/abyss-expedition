package abyss.puzzle;

import java.awt.Point;
import java.util.*;

/** Pure game rules, independent of Swing. Snapshots contain values, never mutable Points. */
public final class PuzzleBoard {
    public static final int SIZE = 8, MOVE_LIMIT = 40;
    public enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);
        final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }
    public record State(int player, int monster, int boxA, int boxB, int remaining) {
        public State { if (boxA > boxB) { int swap = boxA; boxA = boxB; boxB = swap; } }
        boolean box(int cell) { return boxA == cell || boxB == cell; }
    }
    private final Random random;
    private final int coreA, coreB;
    private State state;
    private int moves;
    private int shattered = -1;

    public PuzzleBoard(State state, int coreA, int coreB, Random random) {
        Set<Integer> cells = new HashSet<>(List.of(state.player(), state.monster(), state.boxA(), state.boxB(), coreA, coreB));
        if (cells.size() != 6 || cells.stream().anyMatch(c -> c < 0 || c >= SIZE * SIZE)
                || state.remaining() != 3 || edge(coreA) || edge(coreB))
            throw new IllegalArgumentException("Invalid initial puzzle layout");
        this.state = state; this.coreA = coreA; this.coreB = coreB; this.random = Objects.requireNonNull(random);
    }

    public State state() { return state; }
    public int moves() { return moves; }
    public int cores() { return Integer.bitCount(state.remaining()); }
    public boolean won() { return state.remaining() == 0; }
    public boolean finished() { return won() || moves >= MOVE_LIMIT; }
    public Point player() { return point(state.player()); }
    public Point monster() { return point(state.monster()); }
    public Set<Point> boxes() { return Set.of(point(state.boxA()), point(state.boxB())); }
    public Set<Point> targets() {
        Set<Point> result = new HashSet<>();
        if ((state.remaining() & 1) != 0) result.add(point(coreA));
        if ((state.remaining() & 2) != 0) result.add(point(coreB));
        return result;
    }
    public Point shattered() { return shattered < 0 ? null : point(shattered); }

    public boolean move(Direction direction) {
        if (finished()) return false;
        State next = transition(state, direction, true);
        if (next == null) return false;
        shattered = next.remaining() == state.remaining() ? -1 : next.monster();
        state = next;
        moves++;
        return true;
    }

    /** Solver deliberately excludes random teleports, proving a route independent of luck. */
    State transition(State from, Direction direction, boolean allowRespawn) {
        int next = offset(from.player(), direction), monster = from.monster();
        if (next < 0 || next == monster) return null;
        int a = from.boxA(), b = from.boxB(), mask = from.remaining();
        if (from.box(next)) {
            int beyond = offset(next, direction);
            if (beyond < 0 || from.box(beyond)) return null;
            if (beyond == monster) {
                int after = offset(monster, direction);
                if (after < 0 || from.box(after)) return null;
                monster = after;
                if (a == next) a = beyond; else b = beyond;
                if (edge(monster)) {
                    if (!allowRespawn) return null;
                    monster = freeCell(next, a, b, mask, true);
                } else {
                    if (monster == coreA) mask &= ~1;
                    if (monster == coreB) mask &= ~2;
                }
            } else {
                if (edge(beyond)) {
                    if (!allowRespawn) return null;
                    int other = a == next ? b : a;
                    beyond = centralBox(next, monster, other, mask);
                }
                if (a == next) a = beyond; else b = beyond;
            }
        }
        return new State(next, monster, a, b, mask);
    }

    private boolean target(int cell, int mask) {
        return cell == coreA && (mask & 1) != 0 || cell == coreB && (mask & 2) != 0;
    }
    private int centralBox(int player, int monster, int other, int mask) {
        for (int cell : new int[]{36, 28, 35, 27})
            if (cell != player && cell != monster && cell != other && !target(cell, mask)) return cell;
        return freeCell(player, monster, other, mask, false);
    }
    private int freeCell(int player, int occupiedA, int occupiedB, int mask, boolean central) {
        List<Integer> cells = new ArrayList<>();
        int last = central ? 5 : 6;
        for (int y = 1; y <= last; y++) for (int x = 1; x <= last; x++) {
            int cell = y * SIZE + x;
            if (cell != player && cell != occupiedA && cell != occupiedB && !target(cell, mask)) cells.add(cell);
        }
        if (cells.isEmpty()) throw new IllegalStateException("No free respawn tile");
        return cells.get(random.nextInt(cells.size()));
    }
    int estimate(State s) {
        if (s.remaining() == 0) return 0;
        if (s.remaining() == 1) return distance(s.monster(), coreA);
        if (s.remaining() == 2) return distance(s.monster(), coreB);
        return Math.min(distance(s.monster(), coreA), distance(s.monster(), coreB)) + distance(coreA, coreB);
    }
    static int distance(int a, int b) { return Math.abs(a % SIZE - b % SIZE) + Math.abs(a / SIZE - b / SIZE); }
    static boolean edge(int cell) { return cell % SIZE == 0 || cell % SIZE == SIZE - 1 || cell / SIZE == 0 || cell / SIZE == SIZE - 1; }
    private static int offset(int cell, Direction d) {
        int x = cell % SIZE + d.dx, y = cell / SIZE + d.dy;
        return x < 0 || x >= SIZE || y < 0 || y >= SIZE ? -1 : y * SIZE + x;
    }
    private static Point point(int cell) { return new Point(cell % SIZE, cell / SIZE); }
}

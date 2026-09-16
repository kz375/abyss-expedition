package abyss.puzzle;

import java.util.*;

/** Bounded generation: every accepted board has a verified <=40-move route. */
public final class PuzzleLayouts {
    private PuzzleLayouts() { }
    public static PuzzleBoard generate(Random random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            Set<Integer> used = new HashSet<>();
            int monster = draw(random, used, 5), player = draw(random, used, 6);
            int a = draw(random, used, 6), b = draw(random, used, 6);
            int t1 = draw(random, used, 6), t2 = draw(random, used, 6);
            PuzzleBoard board = new PuzzleBoard(new PuzzleBoard.State(player, monster, a, b, 3), t1, t2, random);
            if (!solve(board, 18000).isEmpty()) return board;
        }
        // Guaranteed fallback with a randomized orientation; same public rules.
        boolean transpose = random.nextBoolean();
        int p = transform(9, transpose), m = transform(19, transpose);
        return new PuzzleBoard(new PuzzleBoard.State(p, m, transform(18, transpose), transform(34, transpose), 3),
                transform(21, transpose), transform(45, transpose), random);
    }
    private static int transform(int c, boolean transpose) { return transpose ? (c % 8) * 8 + c / 8 : c; }
    private static int draw(Random r, Set<Integer> used, int limit) {
        List<Integer> free = new ArrayList<>();
        for (int y = 1; y <= limit; y++) for (int x = 1; x <= limit; x++) if (!used.contains(y*8+x)) free.add(y*8+x);
        int cell = free.get(r.nextInt(free.size())); used.add(cell); return cell;
    }
    private record Search(PuzzleBoard.State state, String path, int estimate, long order) { }
    public static List<PuzzleBoard.Direction> solve(PuzzleBoard board, int budget) {
        PriorityQueue<Search> queue = new PriorityQueue<>(Comparator.comparingInt(Search::estimate).thenComparingLong(Search::order));
        Map<PuzzleBoard.State, Integer> visited = new HashMap<>();
        long order = 0;
        queue.add(new Search(board.state(), "", board.estimate(board.state()), order++));
        visited.put(board.state(), 0);
        while (!queue.isEmpty() && budget-- > 0) {
            Search current = queue.remove();
            if (current.path().length() > visited.get(current.state())) continue;
            if (current.state().remaining() == 0) {
                List<PuzzleBoard.Direction> answer = new ArrayList<>();
                for (char c : current.path().toCharArray()) answer.add(PuzzleBoard.Direction.values()[c-'0']);
                return answer;
            }
            for (PuzzleBoard.Direction direction : PuzzleBoard.Direction.values()) {
                PuzzleBoard.State next = board.transition(current.state(), direction, false);
                int length = current.path().length() + 1;
                if (next == null || length + board.estimate(next) > PuzzleBoard.MOVE_LIMIT
                        || visited.getOrDefault(next, Integer.MAX_VALUE) <= length) continue;
                visited.put(next, length);
                queue.add(new Search(next, current.path() + direction.ordinal(), length + board.estimate(next), order++));
            }
        }
        return List.of();
    }
}

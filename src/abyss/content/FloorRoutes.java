package abyss.content;

import abyss.core.RandomSelection;
import abyss.ui.ConsoleInput;
import java.util.*;

/** Two permanent battles plus four alternatives; choosing one alternative closes the other three. */
public final class FloorRoutes {
    private final int[] types;
    private int visited;
    public FloorRoutes(Random random) {
        List<Integer> side = RandomSelection.sampleDistinct(List.of(0, 1, 2, 3), 4, random);
        types = side.stream().mapToInt(Integer::intValue).toArray();
    }
    public FloorRoutes(int[] types, int visited) {
        if (!valid(types, visited)) throw new IllegalArgumentException("Invalid floor routes");
        this.types = types.clone(); this.visited = visited;
    }
    public static boolean valid(int[] types, int visited) {
        // Two-route saves are valid legacy checkpoints. New expeditions always contain four side routes.
        if (types == null || (types.length != 2 && types.length != 4) || visited < 0 || visited >= (1 << types.length)) return false;
        Set<Integer> unique = new HashSet<>();
        for (int type : types) if (type < 0 || type >= 4 || !unique.add(type)) return false;
        return true;
    }
    public int[] types() { return types.clone(); }
    public int visited() { return visited; }
    public GameNode choose(ConsoleInput input) {
        List<GameNode> sides = GameNodes.sideNodes();
        List<GameNode> nodes = new ArrayList<>(List.of(GameNodes.battle(), GameNodes.eliteBattle()));
        for (int type : types) nodes.add(sides.get(type));
        while (true) {
            System.out.println(abyss.ui.Language.t("Choose your route:"));
            for (int i = 0; i < nodes.size(); i++) {
                boolean used = i >= 2 && (visited & (1 << (i - 2))) != 0;
                System.out.println("  " + (i + 1) + ". " + abyss.ui.Language.t(nodes.get(i).getLabel()) + (used ? abyss.ui.Language.t(" [visited]") : ""));
            }
            int choice = input.readChoice(1, nodes.size()) - 1;
            if (choice < 2) return nodes.get(choice);
            int bit = 1 << (choice - 2);
            if ((visited & bit) != 0) { System.out.println(abyss.ui.Language.t("A random event was already chosen on this floor.")); continue; }
            visited = (1 << types.length) - 1;
            return nodes.get(choice);
        }
    }
}

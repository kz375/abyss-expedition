package abyss.content;

import abyss.core.RandomSelection;
import abyss.ui.ConsoleInput;
import java.util.*;

/** Four stable routes per floor. Each side location is usable once; combat remains available. */
public final class FloorRoutes {
    private final int[] types;
    private int visited;
    public FloorRoutes(Random random) {
        List<Integer> side = RandomSelection.sampleDistinct(List.of(0, 1, 2), 2, random);
        types = new int[]{side.get(0), side.get(1)};
    }
    public FloorRoutes(int[] types, int visited) {
        if (!valid(types, visited)) throw new IllegalArgumentException("Invalid floor routes");
        this.types = types.clone(); this.visited = visited;
    }
    public static boolean valid(int[] types, int visited) {
        return types != null && types.length == 2 && types[0] >= 0 && types[0] < 3
                && types[1] >= 0 && types[1] < 3 && types[0] != types[1] && visited >= 0 && visited <= 3;
    }
    public int[] types() { return types.clone(); }
    public int visited() { return visited; }
    public GameNode choose(ConsoleInput input) {
        List<GameNode> sides = GameNodes.sideNodes();
        List<GameNode> nodes = List.of(GameNodes.battle(), GameNodes.eliteBattle(), sides.get(types[0]), sides.get(types[1]));
        while (true) {
            System.out.println(abyss.ui.Language.t("Choose your route:"));
            for (int i = 0; i < nodes.size(); i++) {
                boolean used = i >= 2 && (visited & (1 << (i - 2))) != 0;
                System.out.println("  " + (i + 1) + ". " + abyss.ui.Language.t(nodes.get(i).getLabel()) + (used ? abyss.ui.Language.t(" [visited]") : ""));
            }
            System.out.println(abyss.ui.Language.t("  5. Leave expedition (return to the saved checkpoint next time)"));
            int choice = input.readChoice(1, 5) - 1;
            if (choice == 4) throw new abyss.core.GameInterruptedException();
            if (choice < 2) return nodes.get(choice);
            int bit = 1 << (choice - 2);
            if ((visited & bit) != 0) { System.out.println(abyss.ui.Language.t("This location has already been visited on this floor.")); continue; }
            visited |= bit;
            return nodes.get(choice);
        }
    }
}

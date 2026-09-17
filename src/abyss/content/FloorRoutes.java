package abyss.content;

import abyss.core.RandomSelection;
import abyss.ui.ConsoleInput;
import java.util.*;

/** Two permanent battles plus four distinct discovery cards; choosing one closes the other three. */
public final class FloorRoutes {
    private static final int EVENT_CODE = 100, CAMP_CODE = 200, SHOP_CODE = 201;
    private final int[] types;
    private int visited;
    private int chosenRoute;
    public FloorRoutes(Random random) {
        List<Integer> deck = new ArrayList<>();
        for (GameEvent event : GameEvent.cardPool()) deck.add(EVENT_CODE + event.ordinal());
        deck.add(CAMP_CODE); deck.add(SHOP_CODE);
        List<Integer> side = RandomSelection.sampleDistinct(deck, 4, random);
        types = side.stream().mapToInt(Integer::intValue).toArray();
        chosenRoute = -1;
    }
    public FloorRoutes(int[] types, int visited) {
        this(types, visited, -1);
    }
    public FloorRoutes(int[] types, int visited, int chosenRoute) {
        if (!valid(types, visited)) throw new IllegalArgumentException("Invalid floor routes");
        if (chosenRoute < -1 || chosenRoute >= types.length) throw new IllegalArgumentException("Invalid selected route");
        this.types = types.clone(); this.visited = visited; this.chosenRoute = chosenRoute;
    }
    public static boolean valid(int[] types, int visited) {
        // One-dot-one save files used 0..3 node ids; their two-route and four-route forms remain valid.
        if (types == null || (types.length != 2 && types.length != 4) || visited < 0 || visited >= (1 << types.length)) return false;
        Set<Integer> unique = new HashSet<>();
        boolean legacy = Arrays.stream(types).allMatch(type -> type >= 0 && type < 4);
        for (int type : types) if (!unique.add(type) || (!legacy && !validCardCode(type))) return false;
        return true;
    }
    private static boolean validCardCode(int code) {
        return code == CAMP_CODE || code == SHOP_CODE
                || code >= EVENT_CODE && code < EVENT_CODE + GameEvent.values().length - 1;
    }
    public int[] types() { return types.clone(); }
    public int visited() { return visited; }
    public int chosenRoute() { return chosenRoute; }
    public GameNode choose(ConsoleInput input) {
        List<GameNode> nodes = new ArrayList<>(List.of(GameNodes.battle(), GameNodes.eliteBattle()));
        for (int type : types) nodes.add(nodeFor(type));
        while (true) {
            System.out.println(abyss.ui.Language.t("Choose your route:"));
            for (int i = 0; i < nodes.size(); i++) {
                int card = i - 2;
                String state = card < 0 || visited == 0 ? "" : card == chosenRoute ? " [chosen]" : " [closed]";
                System.out.println("  " + (i + 1) + ". " + abyss.ui.Language.t(nodes.get(i).getLabel()) + abyss.ui.Language.t(state));
            }
            int choice = input.readChoice(1, nodes.size()) - 1;
            if (choice < 2) return nodes.get(choice);
            int bit = 1 << (choice - 2);
            if ((visited & bit) != 0) { System.out.println(abyss.ui.Language.t("A discovery was already chosen on this floor.")); continue; }
            visited = (1 << types.length) - 1;
            chosenRoute = choice - 2;
            return nodes.get(choice);
        }
    }
    private static GameNode nodeFor(int code) {
        if (code == CAMP_CODE) return GameNodes.campfire();
        if (code == SHOP_CODE) return GameNodes.merchant();
        if (code >= EVENT_CODE) return GameNodes.event(GameEvent.values()[code - EVENT_CODE]);
        // Legacy checkpoints: old 0=event, 1=campfire, 2=merchant, 3=event.
        return switch (code) {
            case 1 -> GameNodes.campfire();
            case 2 -> GameNodes.merchant();
            default -> GameNodes.event(GameEvent.ANCIENT_SHRINE);
        };
    }
}

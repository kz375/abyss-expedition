package abyss.content;

import abyss.combat.Hero;
import abyss.config.Difficulty;

import java.util.List;

/** Standard map-node definitions. */
public final class GameNodes
{
    private GameNodes() { }

    public static List<GameNode> standard()
    {
        return List.of(battle(), eliteBattle(), new EventNode("Unknown Event"), new CampNode(), new ShopNode());
    }

    public static GameNode battle() { return new BattleNode(); }
    public static GameNode eliteBattle() { return new EliteNode(); }
    public static List<GameNode> sideNodes() { return List.of(new EventNode("Unknown Event"), new CampNode(), new ShopNode(), new EventNode("Fate Event")); }
    public static GameNode finalBattle() { return new BossNode(); }

    private static final class BattleNode implements GameNode
    {
        @Override public String getLabel() { return "Battle"; }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d) { return s.resolveBattle(h, f, false, d); }
    }
    private static final class EliteNode implements GameNode
    {
        @Override public String getLabel() { return "Elite Battle"; }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d) { return s.resolveBattle(h, f, true, d); }
    }
    private static final class EventNode implements GameNode
    {
        private final String label;
        private EventNode(String label) { this.label = label; }
        @Override public String getLabel() { return label; }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d) { s.resolveEvent(h, f, d); return false; }
    }
    private static final class CampNode implements GameNode
    {
        @Override public String getLabel() { return "Campfire"; }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d) { s.visitCamp(h); return false; }
    }
    private static final class ShopNode implements GameNode
    {
        @Override public String getLabel() { return "Merchant"; }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d) { s.visitShop(h); return false; }
    }
    private static final class BossNode implements GameNode
    {
        @Override public String getLabel() { return "Final Battle"; }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d)
        {
            System.out.println(abyss.ui.Language.t("The final gate opens. There is no way back."));
            return s.resolveBattle(h, f, false, d);
        }
    }
}

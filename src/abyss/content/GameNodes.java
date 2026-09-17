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
        return List.of(battle(), eliteBattle(), event(GameEvent.ANCIENT_SHRINE), campfire(), merchant());
    }

    public static GameNode battle() { return new BattleNode(); }
    public static GameNode eliteBattle() { return new EliteNode(); }
    public static GameNode event(GameEvent event) { return new EventNode(event); }
    public static GameNode campfire() { return new CampNode(); }
    public static GameNode merchant() { return new ShopNode(); }
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
        private final GameEvent event;
        private EventNode(GameEvent event) { this.event = event; }
        @Override public String getLabel() { return event.cardLabel(); }
        @Override public boolean resolve(GameServices s, Hero h, int f, Difficulty d) { s.resolveEventCard(event, h, f, d); return false; }
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

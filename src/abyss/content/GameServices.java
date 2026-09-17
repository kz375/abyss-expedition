package abyss.content;

import abyss.combat.Hero;
import abyss.config.Difficulty;

/** Operations content nodes request from the game-flow coordinator. */
public interface GameServices
{
    boolean resolveBattle(Hero hero, int floor, boolean elite, Difficulty difficulty);
    void resolveEvent(Hero hero, int floor, Difficulty difficulty);
    /** Legacy event services may still resolve a generic event; the session overrides this for named cards. */
    default void resolveEventCard(GameEvent event, Hero hero, int floor, Difficulty difficulty) { resolveEvent(hero, floor, difficulty); }
    void visitCamp(Hero hero);
    void visitShop(Hero hero);
}

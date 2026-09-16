package abyss.content;

import abyss.combat.Hero;
import abyss.config.Difficulty;

/** Operations content nodes request from the game-flow coordinator. */
public interface GameServices
{
    boolean resolveBattle(Hero hero, int floor, boolean elite, Difficulty difficulty);
    void resolveEvent(Hero hero, int floor, Difficulty difficulty);
    void visitCamp(Hero hero);
    void visitShop(Hero hero);
}

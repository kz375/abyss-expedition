package abyss.content;

import abyss.combat.Hero;
import abyss.config.Difficulty;

import java.util.List;

/** Input, random, reward, and battle operations required by random events. */
public interface EventServices extends GameServices
{
    int readChoice(int minimum, int maximum);
    int nextInt(int bound);
    boolean nextBoolean();
    int gainGold(Hero hero, int amount);
    List<RelicEffect> unownedRelics(Hero hero);
    RelicEffect randomElement(List<RelicEffect> relics);
    void resolveMechanismEncounter(Hero hero, int floor, Difficulty difficulty);
    boolean canEncounterMechanism();
}

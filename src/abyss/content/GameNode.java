package abyss.content;

import abyss.combat.Hero;
import abyss.config.Difficulty;

/** A selectable destination on the expedition map. */
public interface GameNode
{
    String getLabel();
    boolean resolve(GameServices services, Hero hero, int floor, Difficulty difficulty);
}

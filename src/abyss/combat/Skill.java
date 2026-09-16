package abyss.combat;

/** A hero's active combat ability. */
public interface Skill
{
    String getName();

    int getBaseCooldown();

    void use(HeroContext hero, Combatant enemy);
}

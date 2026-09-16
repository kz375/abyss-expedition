package abyss.combat;

/** Special-action strategy used by an enemy tier or boss. */
public interface EnemyBehavior
{
    String getSkillName();

    void useSpecial(HeroContext hero, Combatant enemy);
}

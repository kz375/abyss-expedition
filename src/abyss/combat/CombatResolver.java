package abyss.combat;

/** Engine-provided damage settlement used by portable combat entities and skills. */
public interface CombatResolver
{
    int hitEnemy(HeroContext hero, Enemy enemy, double multiplier);

    int hitHero(HeroContext hero, Enemy enemy, double multiplier);
}

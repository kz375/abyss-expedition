package abyss.combat;

import abyss.config.Difficulty;
import abyss.content.HeroClass;
import abyss.core.RunStatistics;
import java.util.Random;

/** Wires damage resolution to a single expedition's random stream and statistics. */
public final class HeroFactory {
    private HeroFactory() { }
    public static Hero create(String name, HeroClass type, Difficulty difficulty, Random random, RunStatistics stats) {
        CombatResolver resolver = new CombatResolver() {
            public int hitEnemy(HeroContext hero, Enemy enemy, double multiplier) {
                return CombatCalculator.hitEnemy((Hero)hero, enemy, multiplier, stats, random);
            }
            public int hitHero(HeroContext hero, Enemy enemy, double multiplier) {
                return CombatCalculator.hitHero((Hero)hero, enemy, multiplier, stats, random);
            }
        };
        return new Hero(name, type, difficulty, random, resolver, stats);
    }
}

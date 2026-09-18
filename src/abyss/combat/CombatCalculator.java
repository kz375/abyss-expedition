package abyss.combat;

import abyss.content.RelicEffect;
import abyss.core.RunStatistics;

/** Damage settlement, including critical hits, statuses, and relic modifiers. */
public final class CombatCalculator
{
    private CombatCalculator() { }

    public static int hitEnemy(Hero hero, Enemy enemy, double multiplier, RunStatistics statistics, java.util.Random random)
    {
        if (enemy.isProtectedBySummons()) return 0;
        DamageRoll roll = calculate(hero.getCurrentAttack(), enemy.getCurrentDefense(), multiplier,
                15 + hero.getCritBonus(), random);
        boolean critical = roll.critical;
        int damage = roll.damage;
        StatusEffect curse = enemy.findStatus("Curse");
        if (curse != null) damage = damage * (100 + curse.getPower()) / 100;
        StatusEffect empower = hero.findStatus("Empower");
        if (empower != null) damage = damage * (100 + empower.getPower()) / 100;
        for (RelicEffect relic : hero.getEquippedRelics()) damage = relic.modifyOutgoingDamage(hero, enemy, damage);
        int actual = Math.min(enemy.getHealth(), Math.max(0, damage - enemy.getShield()));
        enemy.takeDamage(damage);
        statistics.addDamageDealt(actual);
        for (RelicEffect relic : hero.getEquippedRelics()) relic.onDamageDealt(hero, enemy, actual);
        if (critical) System.out.println(abyss.ui.Language.battle("  >> CRITICAL HIT! <<"));
        return actual;
    }

    public static int hitHero(Hero hero, Enemy enemy, double multiplier, RunStatistics statistics, java.util.Random random)
    {
        int damage = calculate(enemy.getCurrentAttack(), hero.getCurrentDefense(), multiplier, 5, random).damage;
        StatusEffect curse = hero.findStatus("Curse");
        if (curse != null) damage = damage * (100 + curse.getPower()) / 100;
        StatusEffect empower = enemy.findStatus("Empower");
        if (empower != null) damage = damage * (100 + empower.getPower()) / 100;
        for (RelicEffect relic : hero.getEquippedRelics()) damage = relic.modifyIncomingDamage(hero, enemy, damage);
        int actual = Math.min(hero.getHealth(), Math.max(0, damage - hero.getShield()));
        hero.takeDamage(damage);
        statistics.addDamageTaken(actual);
        for (RelicEffect relic : hero.getEquippedRelics()) relic.onDamageTaken(hero, enemy, actual);
        return actual;
    }

    private static DamageRoll calculate(int attack, int defense, double multiplier, int critChance, java.util.Random random)
    {
        int effectiveDefense = Math.max(0, defense - (random.nextBoolean() ? 0 : 1));
        int base = Math.max(1, attack - effectiveDefense);
        boolean critical = random.nextInt(100) < critChance;
        return new DamageRoll(Math.max(1, (int) (base * multiplier * (critical ? 1.5 : 1.0))), critical);
    }

    private record DamageRoll(int damage, boolean critical) { }
}

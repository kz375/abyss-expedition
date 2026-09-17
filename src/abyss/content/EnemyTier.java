package abyss.content;

import abyss.combat.EnemyBehavior;
import abyss.combat.EnemyBehaviors;

import java.util.Random;

/** Base statistics and behavior assignment for non-boss enemies. */
public enum EnemyTier
{
    EARLY(MonsterType.Zone.EARLY, 60, 12, 4),
    MID(MonsterType.Zone.MID, 100, 18, 6),
    LATE(MonsterType.Zone.LATE, 150, 24, 8),
    DEEP(MonsterType.Zone.DEEP, 200, 30, 10);

    private final MonsterType.Zone zone;
    private final int baseHealth;
    private final int baseAttack;
    private final int baseDefense;
    EnemyTier(MonsterType.Zone zone, int baseHealth, int baseAttack, int baseDefense)
    {
        this.zone = zone;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
    }

    public static EnemyTier forFloor(int floor)
    {
        if (floor <= 2) return EARLY;
        if (floor <= 5) return MID;
        if (floor <= 7) return LATE;
        return DEEP;
    }

    public MonsterType randomMonster(Random random) { return MonsterType.random(zone, random); }
    public int getBaseHealth() { return baseHealth; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseDefense() { return baseDefense; }
}

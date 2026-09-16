package abyss.content;

import abyss.combat.EnemyBehavior;
import abyss.combat.EnemyBehaviors;

import java.util.List;
import java.util.Random;

/** Base statistics and behavior assignment for non-boss enemies. */
public enum EnemyTier
{
    EARLY(List.of("Cave Bat", "Abyss Hound", "Lost Miner"), 60, 12, 4, EnemyBehaviors.SAVAGE_BITE),
    MID(List.of("Shadow Assassin", "Cursed Doll", "Bone Scholar"), 100, 18, 6, EnemyBehaviors.CURSED_HEX),
    LATE(List.of("Void Stalker", "Bone Reaper", "Dread Wraith"), 150, 24, 8, EnemyBehaviors.SHADOW_COMBO),
    DEEP(List.of("Abyss Fiend", "Doom Herald"), 200, 30, 10, EnemyBehaviors.ABYSSAL_REND);

    private final List<String> names;
    private final int baseHealth;
    private final int baseAttack;
    private final int baseDefense;
    private final EnemyBehavior behavior;

    EnemyTier(List<String> names, int baseHealth, int baseAttack, int baseDefense, EnemyBehavior behavior)
    {
        this.names = names;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.behavior = behavior;
    }

    public static EnemyTier forFloor(int floor)
    {
        if (floor <= 2) return EARLY;
        if (floor <= 5) return MID;
        if (floor <= 7) return LATE;
        return DEEP;
    }

    public String randomName(Random random) { return names.get(random.nextInt(names.size())); }
    public int getBaseHealth() { return baseHealth; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseDefense() { return baseDefense; }
    public EnemyBehavior getBehavior() { return behavior; }
}

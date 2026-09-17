package abyss.content;

import abyss.combat.EnemyBehavior;
import abyss.combat.EnemyBehaviors;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Stable monster roster shared by spawning and the permanent monster codex. */
public enum MonsterType {
    CAVE_BAT("Cave Bat", "Beast", Zone.EARLY, -6, 1, 0, EnemyBehaviors.SAVAGE_BITE),
    ABYSS_HOUND("Abyss Hound", "Beast", Zone.EARLY, 8, 2, 0, EnemyBehaviors.SAVAGE_BITE),
    LOST_MINER("Lost Miner", "Humanoid", Zone.EARLY, 12, 0, 2, EnemyBehaviors.PICKAXE_CRUSH),
    MIRE_SLIME("Mire Slime", "Aberration", Zone.EARLY, 5, -1, 3, EnemyBehaviors.MIRE_SPIT),
    GRAVE_RAT("Grave Rat", "Undead", Zone.EARLY, -2, 3, -1, EnemyBehaviors.MIRE_SPIT),

    SHADOW_ASSASSIN("Shadow Assassin", "Humanoid", Zone.MID, 0, 3, 0, EnemyBehaviors.SHADOW_COMBO),
    CURSED_DOLL("Cursed Doll", "Construct", Zone.MID, 8, 0, 2, EnemyBehaviors.CURSED_HEX),
    BONE_SCHOLAR("Bone Scholar", "Undead", Zone.MID, -4, 1, 0, EnemyBehaviors.SOUL_BOLT),
    BLOOD_LEECH("Blood Leech", "Beast", Zone.MID, 10, 2, -1, EnemyBehaviors.BLOOD_DRAIN),
    MIRROR_WISP("Mirror Wisp", "Spirit", Zone.MID, -8, 4, -2, EnemyBehaviors.MIRROR_PULSE),

    VOID_STALKER("Void Stalker", "Aberration", Zone.LATE, 0, 3, 0, EnemyBehaviors.VOID_LASH),
    BONE_REAPER("Bone Reaper", "Undead", Zone.LATE, 12, 1, 2, EnemyBehaviors.REAPER_SWEEP),
    DREAD_WRAITH("Dread Wraith", "Spirit", Zone.LATE, -4, 2, -1, EnemyBehaviors.WRAITH_HOWL),
    PLAGUE_CARRIER("Plague Carrier", "Undead", Zone.LATE, 5, 0, 3, EnemyBehaviors.TOXIC_BURST),
    IRON_GOLEM("Iron Golem", "Construct", Zone.LATE, 28, 0, 5, EnemyBehaviors.IRON_SLAM),

    ABYSS_FIEND("Abyss Fiend", "Demon", Zone.DEEP, 0, 3, 0, EnemyBehaviors.ABYSSAL_REND),
    DOOM_HERALD("Doom Herald", "Demon", Zone.DEEP, 12, 1, 2, EnemyBehaviors.DOOM_CHANT),
    ABYSS_SERPENT("Abyss Serpent", "Beast", Zone.DEEP, -5, 4, -1, EnemyBehaviors.SERPENT_COIL),
    STARVED_COLOSSUS("Starved Colossus", "Giant", Zone.DEEP, 38, 2, 5, EnemyBehaviors.COLOSSUS_CRASH),

    RELIC_GUARDIAN("Relic Guardian", "Boss", Zone.BOSS, 0, 0, 0, EnemyBehaviors.GUARDIANS_WRATH),
    ABYSS_LORD("Abyss Lord", "Boss", Zone.BOSS, 0, 0, 0, EnemyBehaviors.ABYSSAL_NOVA);

    public enum Zone { EARLY, MID, LATE, DEEP, BOSS }
    private final String name, category;
    private final Zone zone;
    private final int healthOffset, attackOffset, defenseOffset;
    private final EnemyBehavior behavior;

    MonsterType(String name, String category, Zone zone, int healthOffset, int attackOffset, int defenseOffset, EnemyBehavior behavior) {
        this.name = name; this.category = category; this.zone = zone;
        this.healthOffset = healthOffset; this.attackOffset = attackOffset; this.defenseOffset = defenseOffset;
        this.behavior = behavior;
    }
    public String monsterName() { return name; }
    public String category() { return category; }
    public Zone zone() { return zone; }
    public int healthOffset() { return healthOffset; }
    public int attackOffset() { return attackOffset; }
    public int defenseOffset() { return defenseOffset; }
    public EnemyBehavior behavior() { return behavior; }
    public boolean isBoss() { return zone == Zone.BOSS; }
    public static MonsterType random(Zone zone, Random random) {
        List<MonsterType> choices = Arrays.stream(values()).filter(type -> type.zone == zone).toList();
        return choices.get(random.nextInt(choices.size()));
    }
    /** The final boss may call any codex creature, but never another boss. */
    public static MonsterType randomSummon(Random random) {
        List<MonsterType> choices = Arrays.stream(values()).filter(type -> !type.isBoss()).toList();
        return choices.get(random.nextInt(choices.size()));
    }
    public static MonsterType fromName(String name) {
        String plain = name.startsWith("Elite ") ? name.substring(6) : name;
        return Arrays.stream(values()).filter(type -> type.name.equals(plain)).findFirst().orElse(null);
    }
}

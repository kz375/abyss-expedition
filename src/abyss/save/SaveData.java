package abyss.save;

import abyss.combat.Hero;
import abyss.config.Difficulty;
import abyss.content.RelicEffect;
import abyss.core.RunStatistics;

import java.io.Serial;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Versioned, battle-free checkpoint for one expedition. */
public final class SaveData implements Serializable
{
    @Serial private static final long serialVersionUID = 1L;
    public static final int VERSION = 1;

    private final int version;
    private int[] routeTypes;
    private int visitedRoutes;
    private boolean seeded;
    private long seed;
    private int outcome; // 0: active checkpoint, 1: victory, 2: defeat
    private final int floor;
    private final boolean mechanismEncounterUsed;
    private final Difficulty difficulty;
    private final String heroName;
    private final String heroClassName;
    private final Random random;
    private final int level, experience, experienceToLevel, maxHealth, health, attack, defense;
    private final int gold, potions, skillCooldown, shield, critBonus, potionBonusPct, goldBonusPct, cooldownReduction;
    private final boolean hasPhoenix, phoenixUsed;
    private final List<String> relicNames;
    private final int normalKills, eliteKills, bossKills, damageDealt, damageTaken;
    private final int goldEarned, potionsDrunk, eventsResolved, totalTurns, floorReached;

    private SaveData(int floor, Difficulty difficulty, Hero hero, RunStatistics statistics, Random random, boolean mechanismEncounterUsed)
    {
        this.floor = floor;
        this.version = VERSION;
        this.mechanismEncounterUsed = mechanismEncounterUsed;
        this.difficulty = difficulty;
        this.heroName = hero.getName();
        this.heroClassName = hero.getHeroClass().name();
        this.random = copyRandom(random);
        level = hero.getLevel(); experience = hero.getExperience(); experienceToLevel = hero.getExperienceToLevel();
        maxHealth = hero.getMaxHealth(); health = hero.getHealth(); attack = hero.getAttack(); defense = hero.getDefense();
        gold = hero.getGold(); potions = hero.getPotions(); skillCooldown = hero.getSkillCooldown(); shield = hero.getShield();
        critBonus = hero.getCritBonus(); potionBonusPct = hero.getPotionBonusPct(); goldBonusPct = hero.getGoldBonusPct();
        cooldownReduction = hero.getCooldownReduction(); hasPhoenix = hero.hasPhoenix(); phoenixUsed = hero.hasUsedPhoenix();
        relicNames = new ArrayList<>();
        for (RelicEffect relic : hero.getEquippedRelics()) relicNames.add(relic.getName());
        normalKills = statistics.getNormalKills(); eliteKills = statistics.getEliteKills(); bossKills = statistics.getBossKills();
        damageDealt = statistics.getDamageDealt(); damageTaken = statistics.getDamageTaken();
        goldEarned = statistics.getGoldEarned(); potionsDrunk = statistics.getPotionsDrunk();
        eventsResolved = statistics.getEventsResolved(); totalTurns = statistics.getTotalTurns(); floorReached = statistics.getFloorReached();
    }

    public static SaveData capture(int floor, Difficulty difficulty, Hero hero, RunStatistics statistics, Random random, boolean mechanismEncounterUsed)
    {
        return new SaveData(floor, difficulty, hero, statistics, random, mechanismEncounterUsed);
    }

    public static SaveData capture(int floor, Difficulty difficulty, Hero hero, RunStatistics statistics,
                                   Random random, boolean mechanismEncounterUsed, abyss.content.FloorRoutes routes)
    {
        SaveData data = capture(floor, difficulty, hero, statistics, random, mechanismEncounterUsed);
        if (routes != null) { data.routeTypes = routes.types(); data.visitedRoutes = routes.visited(); }
        return data;
    }

    public abyss.content.FloorRoutes restoreRoutes(Random random)
    {
        return routeTypes == null ? new abyss.content.FloorRoutes(random)
                : new abyss.content.FloorRoutes(routeTypes, visitedRoutes);
    }

    public SaveData withIdentity(long seed, int outcome) {
        this.seeded = true;
        this.seed = seed;
        this.outcome = outcome;
        return this;
    }
    public boolean hasSeed() { return seeded; }
    public long getSeed() { return seed; }
    public int getOutcome() { return outcome; }

    /** Random has no public state accessor; serialization gives the checkpoint its own exact copy. */
    private static Random copyRandom(Random source)
    {
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(bytes))
            {
                output.writeObject(source);
            }
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())))
            {
                return (Random) input.readObject();
            }
        }
        catch (IOException | ClassNotFoundException exception)
        {
            throw new IllegalStateException("Unable to capture random state for this checkpoint.", exception);
        }
    }

    public boolean isCompatible()
    {
        if (version != VERSION || floor < 1 || floor > 8 || difficulty == null || random == null
                || random.getClass() != Random.class || heroName == null || heroName.length() > 200
                || heroClassName == null || relicNames == null || relicNames.size() > 13
                || outcome < 0 || outcome > 2 || (outcome == 2 ? health != 0 : health < 1)
                || maxHealth < 1 || health > maxHealth || attack < 1 || defense < 0
                || level < 1 || level > 1000 || experience < 0 || experienceToLevel != 70 + (level-1)*35
                || experience >= experienceToLevel || gold < 0 || potions < 0 || shield < 0 || skillCooldown < 0
                || critBonus < 0 || potionBonusPct < 0 || goldBonusPct < 0 || cooldownReduction < 0
                || normalKills < 0 || eliteKills < 0 || bossKills < 0 || damageDealt < 0 || damageTaken < 0
                || goldEarned < 0 || potionsDrunk < 0 || eventsResolved < 0 || totalTurns < 0
                || floorReached < 0 || floorReached > 8
                || routeTypes != null && !abyss.content.FloorRoutes.valid(routeTypes, visitedRoutes)) return false;
        try {
            abyss.content.HeroClass type = abyss.content.HeroClass.valueOf(heroClassName);
            if (maxHealth < (int)(type.getBaseHealth() * difficulty.heroStatMultiplier())) return false;
            java.util.Set<String> known = new java.util.HashSet<>();
            for (RelicEffect relic : abyss.content.RelicCatalog.all()) known.add(relic.getName());
            return new java.util.HashSet<>(relicNames).size() == relicNames.size() && known.containsAll(relicNames);
        } catch (IllegalArgumentException e) { return false; }
    }
    public int getFloor() { return floor; }
    public boolean isMechanismEncounterUsed() { return mechanismEncounterUsed; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getHeroName() { return heroName; }
    public String getHeroClassName() { return heroClassName; }
    public Random getRandom() { return copyRandom(random); }
    public List<String> getRelicNames() { return List.copyOf(relicNames); }

    public void restoreHero(Hero hero, List<RelicEffect> relics)
    {
        hero.restoreCheckpoint(level, experience, experienceToLevel, maxHealth, health, attack, defense,
                gold, potions, skillCooldown, shield, critBonus, potionBonusPct, goldBonusPct, cooldownReduction,
                hasPhoenix, phoenixUsed, relics);
    }

    public void restoreStatistics(RunStatistics statistics)
    {
        statistics.restoreCheckpoint(normalKills, eliteKills, bossKills, damageDealt, damageTaken,
                goldEarned, potionsDrunk, eventsResolved, totalTurns, floorReached);
    }
}

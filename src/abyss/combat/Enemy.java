package abyss.combat;

import abyss.config.Difficulty;
import abyss.content.EnemyTier;
import abyss.content.EliteAffix;
import abyss.content.MonsterType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

/** Enemy state, spawning, intent selection, and Ultra Nightmare modifiers. */
public final class Enemy implements Combatant
{
    private static final int HIGHEST_STARTING_HERO_HEALTH = 226;
    private final String name;
    private final boolean boss;
    private final boolean elite;
    private final boolean finalBoss;
    private int health;
    private int maxHealth;
    private int attack;
    private int defense;
    private final int goldReward;
    private final int experienceReward;
    private int shield;
    private int skillCooldown;
    private final EnemyBehavior behavior;
    private int skillChance;
    private final boolean ultraNightmare;
    private final Random random;
    private final IntConsumer damageDealtRecorder;
    private boolean enraged;
    private boolean phaseTwo;
    private boolean phaseThree;
    /** An Ultra Nightmare final boss cannot be harmed while one of its adds lives. */
    private boolean protectedBySummons;
    private final EliteAffix affix;
    private EnemyIntent intent = EnemyIntent.ATTACK;
    private final List<StatusEffect> statuses = new ArrayList<>();

    private Enemy(String name, int health, int attack, int defense, boolean boss, boolean elite, boolean finalBoss,
                  int goldReward, int experienceReward, EnemyBehavior behavior, int skillChance,
                  boolean ultraNightmare, EliteAffix affix, Random random, IntConsumer damageDealtRecorder)
    {
        this.name = name;
        this.health = health;
        this.maxHealth = health;
        this.attack = attack;
        this.defense = defense;
        this.boss = boss;
        this.elite = elite;
        this.finalBoss = finalBoss;
        this.goldReward = goldReward;
        this.experienceReward = experienceReward;
        this.behavior = behavior;
        this.skillChance = skillChance;
        this.ultraNightmare = ultraNightmare;
        this.affix = affix;
        this.random = random;
        this.damageDealtRecorder = damageDealtRecorder;
    }

    public static Enemy spawn(int floor, boolean elite, Difficulty difficulty, int finalFloor, Random random,
                              IntConsumer damageDealtRecorder)
    {
        return spawn(floor, elite, difficulty, finalFloor, random, damageDealtRecorder, true);
    }

    public static Enemy spawnEvent(int floor, boolean elite, Difficulty difficulty, Random random,
                                   IntConsumer damageDealtRecorder)
    {
        return spawn(floor, elite, difficulty, 8, random, damageDealtRecorder, false);
    }

    private static Enemy spawn(int floor, boolean elite, Difficulty difficulty, int finalFloor, Random random,
                               IntConsumer damageDealtRecorder, boolean allowBoss)
    {
        boolean boss = allowBoss && (floor == 3 || floor == finalFloor);
        if (boss)
        {
            boolean finalBoss = floor == finalFloor;
            MonsterType monster = finalBoss ? MonsterType.ABYSS_LORD : MonsterType.RELIC_GUARDIAN;
            return new Enemy(monster.monsterName(),
                    scaled(125 + floor * 32, difficulty.bossRankMultiplier() * 1.15 * difficulty.enemyHealthMultiplier()),
                    scaled(20 + floor * 4, difficulty.bossRankMultiplier() * 1.15 * difficulty.enemyAttackMultiplier()),
                    scaled(6 + floor, 1.15), true, false, finalBoss,
                    scaled(35 + floor * 8, difficulty.rewardMultiplier()),
                    scaled(65 + floor * 18, difficulty.rewardMultiplier()),
                    monster.behavior(), 30,
                    difficulty == Difficulty.ULTRA_NIGHTMARE, EliteAffix.NONE, random, damageDealtRecorder);
        }
        return spawnRegular(EnemyTier.forFloor(floor).randomMonster(random), floor, elite, difficulty, random, damageDealtRecorder);
    }

    /** Uses floor scaling while allowing scripted encounters to choose a specific codex creature. */
    private static Enemy spawnRegular(MonsterType monster, int floor, boolean elite, Difficulty difficulty, Random random,
                                      IntConsumer damageDealtRecorder)
    {
        EnemyTier tier = EnemyTier.forFloor(floor);
        String name = elite ? "Elite " + monster.monsterName() : monster.monsterName();
        double rank = elite ? difficulty.eliteRankMultiplier() * 1.15 : 1.0;
        double healthMultiplier = rank * difficulty.enemyHealthMultiplier();
        // Every early ordinary foe is slightly tougher than even the highest-health starting hero,
        // but is capped so the opening cannot become a health sponge.
        // From floor three onward, repeated combat should consume resources; discoveries become meaningful alternatives.
        if (floor >= 3) healthMultiplier *= difficulty == Difficulty.ULTRA_NIGHTMARE ? 1.08 : 1.18;
        double attackMultiplier = rank * difficulty.enemyAttackMultiplier() * (floor >= 3 ? 1.08 : 1.0);
        int health = scaled(tier.getBaseHealth() + monster.healthOffset() + floor * 14 + random.nextInt(12), healthMultiplier);
        if (!elite && floor <= 2) {
            int strongestStarter = (int) (HIGHEST_STARTING_HERO_HEALTH * difficulty.heroStatMultiplier());
            int target = strongestStarter + (floor == 1 ? 12 : 26);
            health = Math.min(strongestStarter + 80, Math.max(health, target));
        }
        return new Enemy(name, health,
                scaled(tier.getBaseAttack() + monster.attackOffset() + floor * 3, attackMultiplier),
                scaled(Math.max(0, tier.getBaseDefense() + monster.defenseOffset() + floor), elite ? 1.15 : 1.0), false, elite, false,
                scaled(16 + floor * 6, difficulty.rewardMultiplier() * rank),
                scaled(30 + floor * 12, difficulty.rewardMultiplier() * rank),
                monster.behavior(), elite ? 20 : 12, difficulty == Difficulty.ULTRA_NIGHTMARE,
                elite ? EliteAffix.random(random) : EliteAffix.NONE,
                random, damageDealtRecorder);
    }

    private static int scaled(int value, double multiplier) { return Math.max(1, (int) (value * multiplier)); }
    @Override public boolean isAlive() { return health > 0; }
    @Override public String getName() { return name; }
    @Override public int getAttack() { return attack; }
    @Override public int getDefense() { return defense; }
    @Override public int getShield() { return shield; }
    @Override public int getHealth() { return health; }
    @Override public int getMaxHealth() { return maxHealth; }
    @Override public boolean isPlayer() { return false; }
    @Override public List<StatusEffect> getStatuses() { return statuses; }
    public boolean isBoss() { return boss; }
    public boolean isElite() { return elite; }
    public boolean isFinalBoss() { return finalBoss; }
    public boolean isUltraNightmareFinalBoss() { return finalBoss && ultraNightmare; }
    public boolean isProtectedBySummons() { return protectedBySummons; }
    public void setProtectedBySummons(boolean protectedBySummons) { this.protectedBySummons = protectedBySummons; }
    public EnemyBehavior getBehavior() { return behavior; }
    public int getGoldReward() { return goldReward; }
    public int getExperienceReward() { return experienceReward; }
    public EnemyIntent getIntent() { return intent; }
    public EliteAffix getAffix() { return affix; }
    public boolean isPhaseTwo() { return phaseTwo; }
    public boolean isPhaseThree() { return phaseThree; }
    public void setSkillCooldown(int cooldown) { skillCooldown = cooldown; }

    public void decrementSkillCooldown()
    {
        if (skillCooldown > 0) skillCooldown--;
    }

    public void prepareIntent()
    {
        intent = skillCooldown > 0 ? EnemyIntent.RECOVERING
                : random.nextInt(100) < skillChance ? EnemyIntent.SPECIAL : EnemyIntent.ATTACK;
    }

    public void prepareUltraNightmare()
    {
        if (!ultraNightmare) return;
        int ward = maxHealth * (boss ? 25 : elite ? 20 : 15) / 100;
        shield += ward;
        System.out.println(abyss.ui.Language.battle("Ultra Nightmare [Abyssal Ward]: ") + abyss.ui.Language.t(name) + abyss.ui.Language.battle(" gains ") + ward + abyss.ui.Language.battle(" shield."));
    }

    /** Final-boss Ultra Nightmare mechanic: one add appears for every basic attack. */
    public Enemy summonUltraNightmareMinion()
    {
        if (!isUltraNightmareFinalBoss()) throw new IllegalStateException("Only the Ultra Nightmare final boss can summon minions");
        boolean eliteMinion = random.nextInt(100) < 5;
        Enemy minion = spawnRegular(MonsterType.randomSummon(random), 8, eliteMinion,
                Difficulty.ULTRA_NIGHTMARE, random, damageDealtRecorder);
        minion.halveHealthForSummon();
        minion.prepareEliteAffix();
        minion.prepareUltraNightmare();
        minion.prepareIntent();
        return minion;
    }

    /** Adds are deliberately fragile: their displayed and actual health are half of the regular monster's value. */
    private void halveHealthForSummon()
    {
        health = Math.max(1, health / 2);
        maxHealth = health;
    }

    /** Defeating a protecting add permanently reduces the final boss's current and future combat stats by 3%. */
    public void weakenAfterSummonDefeat()
    {
        if (!isUltraNightmareFinalBoss()) return;
        health = Math.max(1, health * 97 / 100);
        maxHealth = Math.max(1, maxHealth * 97 / 100);
        health = Math.min(health, maxHealth);
        attack = Math.max(1, attack * 97 / 100);
        defense = Math.max(0, defense * 97 / 100);
        shield = Math.max(0, shield * 97 / 100);
        skillChance = Math.max(0, skillChance * 97 / 100);
    }

    public void prepareEliteAffix()
    {
        if (affix == EliteAffix.ARMORED)
        {
            int ward = maxHealth * 25 / 100;
            shield += ward;
            System.out.println(abyss.ui.Language.battle("Elite [Armored]: ") + abyss.ui.Language.t(name) + abyss.ui.Language.battle(" gains ") + ward + abyss.ui.Language.battle(" shield."));
        }
        else if (affix == EliteAffix.FRENZIED)
        {
            int bonus = Math.max(1, attack * 20 / 100);
            attack += bonus;
            System.out.println(abyss.ui.Language.battle("Elite [Frenzied]: ") + abyss.ui.Language.t(name) + abyss.ui.Language.battle(" gains ") + bonus + abyss.ui.Language.battle(" attack."));
        }
    }

    public void enterPhaseTwoIfNeeded()
    {
        if (!boss || phaseTwo || health > maxHealth / 2) return;
        phaseTwo = true;
        int ward = maxHealth * 20 / 100;
        int bonus = Math.max(1, attack * 20 / 100);
        shield += ward;
        attack += bonus;
        skillChance = Math.min(100, skillChance + 15);
        System.out.println(abyss.ui.Language.battle("BOSS PHASE II: ") + abyss.ui.Language.t(name) + abyss.ui.Language.battle(" summons ") + ward + abyss.ui.Language.battle(" shield, gains ")
                + bonus + abyss.ui.Language.battle(" attack, and acts more aggressively!"));
    }

    /** The final quarter is a true third phase: maximum health, attack, and defense each rise by fifteen percent. */
    public void enterPhaseThreeIfNeeded()
    {
        if (!boss || phaseThree || health > maxHealth / 4) return;
        phaseThree = true;
        int healthBonus = Math.max(1, maxHealth * 15 / 100);
        maxHealth += healthBonus;
        attack = scaled(attack, 1.15);
        defense = scaled(defense, 1.15);
        skillChance = Math.min(100, skillChance + 15);
        System.out.println(abyss.ui.Language.battle("BOSS PHASE III: ") + abyss.ui.Language.t(name)
                + abyss.ui.Language.battle(" surges with power! Maximum health, attack, and defense rise by 15%."));
    }

    public void enrageIfNeeded()
    {
        if (!ultraNightmare || enraged || health > maxHealth * 45 / 100) return;
        enraged = true;
        int bonus = Math.max(1, attack * 25 / 100);
        attack += bonus;
        System.out.println(abyss.ui.Language.battle("Ultra Nightmare [Abyssal Rage]: ") + abyss.ui.Language.t(name) + abyss.ui.Language.battle(" gains ") + bonus + abyss.ui.Language.battle(" attack."));
    }

    @Override public void restoreHealth(int amount) { health = Math.min(maxHealth, health + amount); }

    @Override public void takeDamage(int amount)
    {
        if (amount < 0) throw new IllegalArgumentException("Damage cannot be negative");
        if (protectedBySummons) return;
        int blocked = Math.min(shield, amount);
        shield -= blocked;
        health = Math.max(0, health - amount + blocked);
        if (blocked > 0) System.out.println(abyss.ui.Language.t(name) + abyss.ui.Language.battle("'s shield absorbs ") + blocked + abyss.ui.Language.battle(" damage."));
    }

    @Override public void sufferStatusDamage(int amount)
    {
        if (protectedBySummons) return;
        int lost = Math.min(health, Math.max(0, amount - shield));
        takeDamage(amount);
        damageDealtRecorder.accept(lost);
    }
}

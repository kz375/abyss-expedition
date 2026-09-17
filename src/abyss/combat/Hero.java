package abyss.combat;

import abyss.config.Difficulty;
import abyss.content.HeroClass;
import abyss.content.RelicEffect;
import abyss.core.RunStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Player state and progression for one expedition. */
public final class Hero implements HeroContext
{
    private final String name;
    private final HeroClass heroClass;
    private final Difficulty difficulty;
    private final Skill skill;
    private final Random random;
    private final CombatResolver resolver;
    private final RunStatistics statistics;
    private final int startingHealth;
    private int level = 1, experience, experienceToLevel = 70, maxHealth, health, attack, defense;
    private int gold = 35, potions = 2, skillCooldown, shield;
    private int critBonus, potionBonusPct, goldBonusPct, cooldownReduction;
    private boolean hasPhoenix, phoenixUsed;
    private final List<RelicEffect> relics = new ArrayList<>();
    private final List<StatusEffect> statuses = new ArrayList<>();

    public Hero(String name, HeroClass heroClass, Difficulty difficulty, Random random,
                CombatResolver resolver, RunStatistics statistics)
    {
        this.name = name;
        this.heroClass = heroClass;
        this.difficulty = difficulty;
        this.skill = HeroSkillCatalog.forClassName(heroClass.getName());
        this.random = random;
        this.resolver = resolver;
        this.statistics = statistics;
        double multiplier = difficulty.heroStatMultiplier();
        maxHealth = (int) (heroClass.getBaseHealth() * multiplier);
        startingHealth = maxHealth;
        health = maxHealth;
        attack = (int) (heroClass.getBaseAttack() * multiplier);
        defense = (int) (heroClass.getBaseDefense() * multiplier);
    }

    @Override public boolean isAlive() { return health > 0; }
    @Override public String getName() { return name; }
    @Override public int getAttack() { return attack; }
    @Override public int getDefense() { return defense; }
    @Override public int getShield() { return shield; }
    @Override public int getHealth() { return health; }
    @Override public int getMaxHealth() { return maxHealth; }
    @Override public boolean isPlayer() { return true; }
    @Override public List<StatusEffect> getStatuses() { return statuses; }
    public HeroClass getHeroClass() { return heroClass; }
    public Difficulty getDifficulty() { return difficulty; }
    public Skill getSkill() { return skill; }
    @Override public int getLevel() { return level; }
    @Override public int getCritBonus() { return critBonus; }
    @Override public int getPotionBonusPct() { return potionBonusPct; }
    @Override public int getGoldBonusPct() { return goldBonusPct; }
    @Override public int getCooldownReduction() { return cooldownReduction; }
    @Override public int getGold() { return gold; }
    @Override public int getPotions() { return potions; }
    @Override public int getSkillCooldown() { return skillCooldown; }
    @Override public List<RelicEffect> getEquippedRelics() { return List.copyOf(relics); }
    public int getRelicCount() { return relics.size(); }
    public int getExperience() { return experience; }
    public int getExperienceToLevel() { return experienceToLevel; }
    public boolean hasPhoenix() { return hasPhoenix; }
    public boolean hasUsedPhoenix() { return phoenixUsed; }
    public int getStartingHealth() { return startingHealth; }

    public boolean ownsRelic(String relicName)
    {
        for (RelicEffect relic : relics) if (relic.getName().equals(relicName)) return true;
        return false;
    }

    public void equipRelic(RelicEffect relic)
    {
        if (ownsRelic(relic.getName())) return;
        relics.add(relic);
        relic.onEquip(this);
        System.out.println(abyss.ui.Language.battle("Obtained [") + abyss.ui.Language.t(relic.getName()) + abyss.ui.Language.battle("] - ") + abyss.ui.Language.t(relic.getDescription()) + "!");
    }
    public RelicEffect loseRandomRelic(Random random)
    {
        if (relics.isEmpty()) return null;
        RelicEffect relic = relics.remove(random.nextInt(relics.size()));
        relic.onUnequip(this);
        return relic;
    }
    public void healToFull() { health = maxHealth; }
    public void limitShield(int maximum) { shield = Math.min(shield, Math.max(0, maximum)); }
    public void disablePhoenix() { hasPhoenix = false; phoenixUsed = false; }
    public void applyMechanismFailure()
    {
        health = Math.max(startingHealth, health - Math.max(1, (int) Math.ceil(health * 0.10)));
        int lostGold = (int) Math.ceil(gold * 0.05);
        gold = Math.max(0, gold - lostGold);
    }

    @Override public void restoreHealth(int amount) { health = Math.min(maxHealth, health + Math.max(0, amount)); }

    /** Voluntary health payments bypass shields and cannot kill the adventurer. */
    public boolean spendHealth(int amount) {
        if (amount < 0 || health <= amount) return false;
        health -= amount;
        return true;
    }

    @Override public void takeDamage(int amount)
    {
        if (amount < 0) throw new IllegalArgumentException("Damage cannot be negative");
        int blocked = Math.min(shield, amount);
        shield -= blocked;
        health = Math.max(0, health - amount + blocked);
        if (blocked > 0) System.out.println(abyss.ui.Language.battle("Your shield absorbs ") + blocked + abyss.ui.Language.battle(" damage."));
        if (health <= 0 && hasPhoenix && !phoenixUsed)
        {
            phoenixUsed = true;
            health = maxHealth * 2 / 5;
            System.out.println(abyss.ui.Language.battle("[Phoenix Feather] blazes to life! You are revived at ") + health + abyss.ui.Language.battle(" health!"));
        }
    }

    @Override public void sufferStatusDamage(int amount) {
        int lost = Math.min(health, Math.max(0, amount - shield));
        takeDamage(amount); statistics.addDamageTaken(lost);
    }

    public void gainExperience(int amount)
    {
        experience += amount;
        System.out.println(abyss.ui.Language.battle("Gained ") + amount + abyss.ui.Language.battle(" experience."));
        while (experience >= experienceToLevel)
        {
            experience -= experienceToLevel;
            level++;
            experienceToLevel += 35;
            maxHealth += 18; attack += 4; defense += 2; health = maxHealth;
            System.out.println(abyss.ui.Language.battle(">> Reached level ") + level + abyss.ui.Language.battle("! Health, attack, and defense increased. Health fully restored. <<"));
        }
    }

    @Override public void addShield(int amount) { shield += amount; }
    @Override public void addAttack(int amount) { attack += amount; }
    @Override public void addDefense(int amount) { defense += amount; }
    @Override public void addMaxHealth(int amount) { maxHealth = Math.max(startingHealth, maxHealth + amount); health = Math.min(health, maxHealth); }
    @Override public void addCritBonus(int amount) { critBonus += amount; }
    @Override public void addPotionBonusPct(int amount) { potionBonusPct += amount; }
    @Override public void addGoldBonusPct(int amount) { goldBonusPct += amount; }
    @Override public void addCooldownReduction(int amount) { cooldownReduction += amount; }
    @Override public void enablePhoenix() { hasPhoenix = true; }
    @Override public void addGold(int amount) { gold += amount; }
    @Override public void spendGold(int amount) { gold -= amount; }
    @Override public void receivePotion() { potions++; }
    @Override public void recordDamageDealt(int amount) { statistics.addDamageDealt(amount); }
    @Override public int dealDamage(Combatant enemy, double multiplier) { return resolver.hitEnemy(this, (Enemy) enemy, multiplier); }
    @Override public int receiveDamageFrom(Combatant enemy, double multiplier) { return resolver.hitHero(this, (Enemy) enemy, multiplier); }
    @Override public boolean rollChance(int percent) { return random.nextInt(100) < percent; }
    @Override public boolean canStun(Combatant enemy)
    {
        Enemy target = (Enemy) enemy;
        if (target.isBoss() && random.nextInt(100) < 50)
        {
            System.out.println(abyss.ui.Language.t(target.getName()) + abyss.ui.Language.battle(" shrugs off the stun!"));
            return false;
        }
        return true;
    }
    public void clearStatuses() { statuses.clear(); }
    public void resetPhoenix() { phoenixUsed = false; }
    public void drinkPotion()
    {
        if (potions <= 0) return;
        potions--; statistics.addPotionDrunk();
        int heal = (40 + level * 8) * (100 + potionBonusPct) / 100;
        restoreHealth(heal); applyStatus("Regen", 2, 5, false);
        System.out.println(abyss.ui.Language.battle("You drink a potion and restore ") + heal + abyss.ui.Language.battle(" health (Regen 2 turns)."));
    }
    public void startSkillCooldown() { skillCooldown = Math.max(1, skill.getBaseCooldown() - cooldownReduction); }
    public void decrementSkillCooldown() { if (skillCooldown > 0) skillCooldown--; }

    /** Restores a checkpoint captured between floors. Battle-only statuses are intentionally not saved. */
    public void restoreCheckpoint(int level, int experience, int experienceToLevel, int maxHealth, int health,
                                  int attack, int defense, int gold, int potions, int skillCooldown, int shield,
                                  int critBonus, int potionBonusPct, int goldBonusPct, int cooldownReduction,
                                  boolean hasPhoenix, boolean phoenixUsed, List<RelicEffect> equippedRelics)
    {
        this.level = level;
        this.experience = experience;
        this.experienceToLevel = experienceToLevel;
        this.maxHealth = maxHealth;
        this.health = health;
        this.attack = attack;
        this.defense = defense;
        this.gold = gold;
        this.potions = potions;
        this.skillCooldown = skillCooldown;
        this.shield = shield;
        this.critBonus = critBonus;
        this.potionBonusPct = potionBonusPct;
        this.goldBonusPct = goldBonusPct;
        this.cooldownReduction = cooldownReduction;
        this.hasPhoenix = hasPhoenix;
        this.phoenixUsed = phoenixUsed;
        relics.clear();
        relics.addAll(equippedRelics);
        statuses.clear();
    }
}

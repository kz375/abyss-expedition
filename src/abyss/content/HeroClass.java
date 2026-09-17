package abyss.content;

import abyss.combat.Combatant;
import abyss.combat.HeroContext;

/** Hero archetypes, their base statistics, and passive hooks. */
public enum HeroClass
{
    WARRIOR("Warrior", 220, 32, 12, "Armor Break", "Iron Will",
            "Deal 240% attack damage and sunder enemy defense", "Start every battle with 70 shield")
    {
        @Override public void onBattleStart(HeroContext hero, Combatant enemy)
        {
            hero.addShield(70);
            System.out.println(abyss.ui.Language.battle("Passive [Iron Will]: You begin with 70 shield."));
        }
    },
    MAGE("Mage", 188, 35, 10, "Arcane Burst", "Mana Flow",
            "Deal 270% attack damage and set the enemy ablaze", "Skills have 1 less cooldown turn"),
    RANGER("Ranger", 214, 36, 11, "Double Shot", "Hunter's Focus",
            "Two arrows (140% each) that mark the target (+15% damage taken)",
            "15% chance for a small bonus hit after attacking")
    {
        @Override public void onBasicAttack(HeroContext hero, Combatant enemy)
        {
            if (enemy.isAlive() && hero.rollChance(15))
            {
                int damage = Math.max(1, hero.getAttack() / 3);
                int actual = Math.min(enemy.getHealth(), Math.max(0, damage - enemy.getShield()));
                enemy.takeDamage(damage);
                hero.recordDamageDealt(actual);
                System.out.println(abyss.ui.Language.battle("Passive [Hunter's Focus]: A bonus hit deals ") + damage + abyss.ui.Language.battle(" damage."));
            }
        }
    },
    CREATOR("Creator", 180, 31, 10, "Reality Rend", "Execution",
            "Five strikes (100%-140%) and Empower yourself (+8% damage, 1 turn)",
            "Instantly destroy enemies below 40 health")
    {
        @Override public boolean tryExecution(HeroContext hero, Combatant enemy)
        {
            if (enemy.getHealth() < 40 && enemy.isAlive())
            {
                System.out.println(abyss.ui.Language.battle("Passive [Execution]: ") + abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" has less than 40 health and is erased."));
                int health = enemy.getHealth();
                enemy.takeDamage(health + enemy.getShield());
                hero.recordDamageDealt(health);
                return true;
            }
            return false;
        }
    },
    PALADIN("Paladin", 226, 31, 13, "Holy Judgment", "Divine Aegis",
            "Deal 215% damage, restore health, and Stun the enemy for 1 turn",
            "Gain 10 shield at the start of every turn")
    {
        @Override public void onTurnStart(HeroContext hero)
        {
            hero.addShield(10);
            System.out.println(abyss.ui.Language.battle("Passive [Divine Aegis]: You gain 10 shield."));
        }
    },
    NECROMANCER("Necromancer", 200, 32, 10, "Soul Drain", "Curse",
            "Deal 200% damage, drain 50% of it as health, and inflict Poison",
            "Enemies start battle Cursed: +25% damage taken for 3 turns")
    {
        @Override public void onBattleStart(HeroContext hero, Combatant enemy)
        {
            enemy.applyStatus("Curse", 3, 25, false);
            System.out.println(abyss.ui.Language.battle("Passive [Curse]: ") + abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" takes 25% more damage for 3 turns."));
        }
    };

    private final String name;
    private final int baseHealth;
    private final int baseAttack;
    private final int baseDefense;
    private final String skillName;
    private final String passiveSkillName;
    private final String skillDescription;
    private final String passiveSkillDescription;

    HeroClass(String name, int baseHealth, int baseAttack, int baseDefense, String skillName, String passiveSkillName,
              String skillDescription, String passiveSkillDescription)
    {
        this.name = name;
        this.baseHealth = baseHealth;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.skillName = skillName;
        this.passiveSkillName = passiveSkillName;
        this.skillDescription = skillDescription;
        this.passiveSkillDescription = passiveSkillDescription;
    }

    public String getName() { return name; }
    public int getBaseHealth() { return baseHealth; }
    public int getBaseAttack() { return baseAttack; }
    public int getBaseDefense() { return baseDefense; }
    public String getSkillName() { return skillName; }
    public String getPassiveSkillName() { return passiveSkillName; }
    public String getSkillDescription() { return skillDescription; }
    public String getPassiveSkillDescription() { return passiveSkillDescription; }
    public void onBattleStart(HeroContext hero, Combatant enemy) { }
    public void onTurnStart(HeroContext hero) { }
    public void onBasicAttack(HeroContext hero, Combatant enemy) { }
    public boolean tryExecution(HeroContext hero, Combatant enemy) { return false; }
}

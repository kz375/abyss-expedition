package abyss.combat;

/** Factory for the six current hero skills. */
public final class HeroSkillCatalog
{
    private HeroSkillCatalog()
    {
    }

    public static Skill forClassName(String heroClassName)
    {
        return switch (heroClassName)
        {
            case "Warrior" -> new WarriorSkill();
            case "Mage" -> new MageSkill();
            case "Ranger" -> new RangerSkill();
            case "Creator" -> new CreatorSkill();
            case "Paladin" -> new PaladinSkill();
            case "Necromancer" -> new NecromancerSkill();
            default -> throw new IllegalArgumentException("Unknown hero class: " + heroClassName);
        };
    }

    private abstract static class BaseSkill implements Skill
    {
        private final String name;
        private final int cooldown;

        private BaseSkill(String name, int cooldown)
        {
            this.name = name;
            this.cooldown = cooldown;
        }

        @Override public String getName() { return name; }
        @Override public int getBaseCooldown() { return cooldown; }
    }

    private static final class WarriorSkill extends BaseSkill
    {
        private WarriorSkill() { super("Armor Break", 2); }
        @Override public void use(HeroContext hero, Combatant enemy)
        {
            System.out.println(abyss.ui.Language.battle("Casting [Armor Break]!"));
            int damage = hero.dealDamage(enemy, 2.4);
            enemy.applyStatus("Sunder", 2, 5, false);
            System.out.println(abyss.ui.Language.battle("Armor Break deals ") + damage + abyss.ui.Language.battle(" damage and sunders ") + abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle("'s armor!"));
        }
    }

    private static final class MageSkill extends BaseSkill
    {
        private MageSkill() { super("Arcane Burst", 1); }
        @Override public void use(HeroContext hero, Combatant enemy)
        {
            System.out.println(abyss.ui.Language.battle("Casting [Arcane Burst]!"));
            int damage = hero.dealDamage(enemy, 2.7);
            int burnPower = 4 + hero.getLevel();
            enemy.applyStatus("Burn", 3, burnPower, false);
            System.out.println(abyss.ui.Language.battle("Arcane Burst deals ") + damage + abyss.ui.Language.battle(" damage and sets the foe ablaze (Burn 3 turns)."));
        }
    }

    private static final class RangerSkill extends BaseSkill
    {
        private RangerSkill() { super("Double Shot", 2); }
        @Override public void use(HeroContext hero, Combatant enemy)
        {
            System.out.println(abyss.ui.Language.battle("Casting [Double Shot]!"));
            int first = hero.dealDamage(enemy, 1.4);
            int second = enemy.isAlive() ? hero.dealDamage(enemy, 1.4) : 0;
            enemy.applyStatus("Curse", 2, 15, false);
            System.out.println(abyss.ui.Language.battle("The two arrows deal ") + first + abyss.ui.Language.battle(" and ") + second + abyss.ui.Language.battle(" damage, and mark the target (+15% damage taken)."));
        }
    }

    private static final class CreatorSkill extends BaseSkill
    {
        private CreatorSkill() { super("Reality Rend", 2); }
        @Override public void use(HeroContext hero, Combatant enemy)
        {
            System.out.println(abyss.ui.Language.battle("Casting [Reality Rend]!"));
            // Signature five-hit skill, tuned below the other high-difficulty burst options.
            double[] multipliers = {1.0, 1.1, 1.2, 1.3, 1.4};
            int total = 0;
            for (int index = 0; index < multipliers.length && enemy.isAlive(); index++)
            {
                int damage = hero.dealDamage(enemy, multipliers[index]);
                total += damage;
                System.out.println(abyss.ui.Language.battle("Reality Rend hit ") + (index + 1) + abyss.ui.Language.battle(" deals ") + damage + abyss.ui.Language.battle(" damage."));
            }
            hero.applyStatus("Empower", 1, 8, false);
            System.out.println(abyss.ui.Language.battle("Reality Rend deals ") + total + abyss.ui.Language.battle(" total damage. Reality bends in your favor (+8% damage, 1 turn)."));
        }
    }

    private static final class PaladinSkill extends BaseSkill
    {
        private PaladinSkill() { super("Holy Judgment", 2); }
        @Override public void use(HeroContext hero, Combatant enemy)
        {
            System.out.println(abyss.ui.Language.battle("Casting [Holy Judgment]!"));
            int damage = hero.dealDamage(enemy, 2.15);
            int heal = 12 + hero.getLevel() * 3;
            hero.restoreHealth(heal);
            System.out.println(abyss.ui.Language.battle("Holy Judgment deals ") + damage + abyss.ui.Language.battle(" damage and restores ") + heal + abyss.ui.Language.battle(" health."));
            if (enemy.isAlive() && hero.canStun(enemy))
            {
                enemy.applyStatus("Stun", 1, 0, false);
                System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" is stunned for 1 turn!"));
            }
        }
    }

    private static final class NecromancerSkill extends BaseSkill
    {
        private NecromancerSkill() { super("Soul Drain", 2); }
        @Override public void use(HeroContext hero, Combatant enemy)
        {
            System.out.println(abyss.ui.Language.battle("Casting [Soul Drain]!"));
            int damage = hero.dealDamage(enemy, 2.0);
            int heal = damage / 2;
            hero.restoreHealth(heal);
            enemy.applyStatus("Poison", 3, 6, false);
            System.out.println(abyss.ui.Language.battle("Soul Drain deals ") + damage + abyss.ui.Language.battle(" damage, drains ") + heal + abyss.ui.Language.battle(" health, and inflicts Poison (3 turns)."));
        }
    }
}

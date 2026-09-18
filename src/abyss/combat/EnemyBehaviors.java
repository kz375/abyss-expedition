package abyss.combat;

/** Reusable enemy special-action definitions. */
public final class EnemyBehaviors
{
    private EnemyBehaviors()
    {
    }

    public static final EnemyBehavior SAVAGE_BITE = new DamageBehavior("Savage Bite", 1.25, null, 0);
    public static final EnemyBehavior PICKAXE_CRUSH = new DamageBehavior("Pickaxe Crush", 1.20, "Sunder", 2);
    public static final EnemyBehavior MIRE_SPIT = new DamageBehavior("Mire Spit", 1.00, "Poison", 3);
    public static final EnemyBehavior CURSED_HEX = new DamageBehavior("Cursed Hex", 1.1, "Weak", 5);
    public static final EnemyBehavior SHADOW_COMBO = new ShadowCombo();
    public static final EnemyBehavior SOUL_BOLT = new DamageBehavior("Soul Bolt", 1.15, "Burn", 3);
    public static final EnemyBehavior BLOOD_DRAIN = new BloodDrain();
    public static final EnemyBehavior MIRROR_PULSE = new DamageBehavior("Mirror Pulse", 1.05, "Curse", 10);
    public static final EnemyBehavior VOID_LASH = new DamageBehavior("Void Lash", 1.30, "Curse", 12);
    public static final EnemyBehavior REAPER_SWEEP = new DamageBehavior("Reaper Sweep", 1.35, null, 0);
    public static final EnemyBehavior WRAITH_HOWL = new DamageBehavior("Wraith Howl", 0.95, "Weak", 7);
    public static final EnemyBehavior TOXIC_BURST = new DamageBehavior("Toxic Burst", 1.10, "Poison", 5);
    public static final EnemyBehavior IRON_SLAM = new DamageBehavior("Iron Slam", 1.25, "Sunder", 4);
    public static final EnemyBehavior ABYSSAL_REND = new DamageBehavior("Abyssal Rend", 1.5, "Burn", 5);
    public static final EnemyBehavior DOOM_CHANT = new DamageBehavior("Doom Chant", 1.10, "Curse", 18);
    public static final EnemyBehavior SERPENT_COIL = new DamageBehavior("Serpent Coil", 1.20, "Poison", 7);
    public static final EnemyBehavior COLOSSUS_CRASH = new DamageBehavior("Colossus Crash", 1.55, "Sunder", 5);
    public static final EnemyBehavior GUARDIANS_WRATH = new DamageBehavior("Guardian's Wrath", 1.4, "Weak", 4);
    public static final EnemyBehavior ABYSSAL_NOVA = new AbyssalNova();

    private static final class DamageBehavior implements EnemyBehavior
    {
        private final String name;
        private final double multiplier;
        private final String status;
        private final int power;

        private DamageBehavior(String name, double multiplier, String status, int power)
        {
            this.name = name;
            this.multiplier = multiplier;
            this.status = status;
            this.power = power;
        }

        @Override public String getSkillName() { return name; }

        @Override public void useSpecial(HeroContext hero, Combatant enemy)
        {
            int damage = hero.receiveDamageFrom(enemy, multiplier);
            if (status != null)
            {
                hero.applyStatus(status, 2, power, false);
                String effect = switch (status) {
                    case "Weak" -> abyss.ui.Language.battle("your attack is weakened (-") + power + abyss.ui.Language.battle(", 2 turns)!");
                    case "Burn" -> abyss.ui.Language.battle("you are set ablaze!");
                    case "Poison" -> abyss.ui.Language.battle("you are poisoned!");
                    case "Sunder" -> abyss.ui.Language.battle("your defense is sundered!");
                    case "Curse" -> abyss.ui.Language.battle("you are cursed!");
                    default -> abyss.ui.Language.battle("you are afflicted!");
                };
                System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [") + abyss.ui.Language.t(name) + abyss.ui.Language.battle("]: ") + damage + abyss.ui.Language.battle(" damage and ") + effect);
            }
            else
            {
                System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [") + abyss.ui.Language.t(name) + abyss.ui.Language.battle("] and deals ") + damage + abyss.ui.Language.battle(" damage!"));
            }
        }
    }

    private static final class BloodDrain implements EnemyBehavior
    {
        @Override public String getSkillName() { return "Blood Drain"; }
        @Override public void useSpecial(HeroContext hero, Combatant enemy)
        {
            int damage = hero.receiveDamageFrom(enemy, 1.15);
            // Reflected damage may have killed the attacker; draining cannot revive it.
            int healing = enemy.isAlive() ? damage / 2 : 0;
            enemy.restoreHealth(healing);
            System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [Blood Drain]: ") + damage
                    + abyss.ui.Language.battle(" damage and restores ") + healing + abyss.ui.Language.battle(" health."));
        }
    }

    private static final class ShadowCombo implements EnemyBehavior
    {
        @Override public String getSkillName() { return "Shadow Combo"; }
        @Override public void useSpecial(HeroContext hero, Combatant enemy)
        {
            int first = hero.receiveDamageFrom(enemy, 0.8);
            int second = hero.isAlive() && enemy.isAlive() ? hero.receiveDamageFrom(enemy, 0.8) : 0;
            System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [Shadow Combo]: two strikes deal ") + first + abyss.ui.Language.battle(" and ") + second + abyss.ui.Language.battle(" damage!"));
        }
    }

    private static final class AbyssalNova implements EnemyBehavior
    {
        @Override public String getSkillName() { return "Abyssal Nova"; }
        @Override public void useSpecial(HeroContext hero, Combatant enemy)
        {
            int damage = hero.receiveDamageFrom(enemy, 1.55);
            hero.applyStatus("Burn", 2, 6, false);
            enemy.applyStatus("Empower", 2, 25, false);
            System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [Abyssal Nova]: ") + damage
                    + abyss.ui.Language.battle(" damage, you are burning, and the abyss empowers him!"));
        }
    }
}

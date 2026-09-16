package abyss.combat;

/** Reusable enemy special-action definitions. */
public final class EnemyBehaviors
{
    private EnemyBehaviors()
    {
    }

    public static final EnemyBehavior SAVAGE_BITE = new DamageBehavior("Savage Bite", 1.25, null, 0);
    public static final EnemyBehavior CURSED_HEX = new DamageBehavior("Cursed Hex", 1.1, "Weak", 5);
    public static final EnemyBehavior SHADOW_COMBO = new ShadowCombo();
    public static final EnemyBehavior ABYSSAL_REND = new DamageBehavior("Abyssal Rend", 1.5, "Burn", 5);
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
                String effect = status.equals("Weak") ? abyss.ui.Language.battle("your attack is weakened (-") + power + abyss.ui.Language.battle(", 2 turns)!") : abyss.ui.Language.battle("you are set ablaze!");
                System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [") + abyss.ui.Language.t(name) + abyss.ui.Language.battle("]: ") + damage + abyss.ui.Language.battle(" damage and ") + effect);
            }
            else
            {
                System.out.println(abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle(" uses [") + abyss.ui.Language.t(name) + abyss.ui.Language.battle("] and deals ") + damage + abyss.ui.Language.battle(" damage!"));
            }
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

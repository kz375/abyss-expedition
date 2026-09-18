package abyss.content;

import abyss.combat.Combatant;
import abyss.combat.HeroContext;

import java.util.List;

/** Catalog of all relic definitions. */
public final class RelicCatalog
{
    private RelicCatalog()
    {
    }

    public static List<RelicEffect> all()
    {
        return List.of(new BloodRuby(), new IronSigil(), new MoonVial(), new VampireFang(), new ThornMail(),
                new LuckyClover(), new AlchemistGourd(), new GoldIdol(), new BattleStandard(), new ChronoCharm(),
                new PhoenixFeather(), new BerserkerMask(), new SerpentRing());
    }

    private abstract static class BaseRelic implements RelicEffect
    {
        private final String name;
        private final String description;

        private BaseRelic(String name, String description)
        {
            this.name = name;
            this.description = description;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return description; }
    }

    private static final class BloodRuby extends BaseRelic
    {
        private BloodRuby() { super("Blood Ruby", "Attack +5"); }
        @Override public void onEquip(HeroContext hero) { hero.addAttack(5); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addAttack(-5); }
    }

    private static final class IronSigil extends BaseRelic
    {
        private IronSigil() { super("Iron Sigil", "Defense +4"); }
        @Override public void onEquip(HeroContext hero) { hero.addDefense(4); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addDefense(-4); }
    }

    private static final class MoonVial extends BaseRelic
    {
        private MoonVial() { super("Moon Vial", "Max health +30 and restore 30 health"); }
        @Override public void onEquip(HeroContext hero) { hero.addMaxHealth(30); hero.restoreHealth(30); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addMaxHealth(-30); }
    }

    private static final class VampireFang extends BaseRelic
    {
        private VampireFang() { super("Vampire Fang", "Heal 15% of all damage you deal"); }
        @Override public void onDamageDealt(HeroContext hero, Combatant enemy, int damage)
        {
            int heal = damage * 15 / 100;
            if (heal > 0)
            {
                hero.restoreHealth(heal);
                System.out.println(abyss.ui.Language.battle("[Vampire Fang] drains ") + heal + abyss.ui.Language.battle(" health."));
            }
        }
    }

    private static final class ThornMail extends BaseRelic
    {
        private ThornMail() { super("Thorn Mail", "Reflect 20% of damage taken"); }
        @Override public void onDamageTaken(HeroContext hero, Combatant enemy, int damage)
        {
            if (damage <= 0 || !enemy.isAlive()) return;
            int thorns = Math.max(1, damage * 20 / 100);
            int before = enemy.getHealth();
            enemy.takeDamage(thorns);
            hero.recordDamageDealt(Math.max(0, before - enemy.getHealth()));
            System.out.println(abyss.ui.Language.battle("[Thorn Mail] reflects ") + thorns + abyss.ui.Language.battle(" damage."));
        }
    }

    private static final class LuckyClover extends BaseRelic
    {
        private LuckyClover() { super("Lucky Clover", "Critical hit chance +10%"); }
        @Override public void onEquip(HeroContext hero) { hero.addCritBonus(10); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addCritBonus(-10); }
    }

    private static final class AlchemistGourd extends BaseRelic
    {
        private AlchemistGourd() { super("Alchemist Gourd", "Potions restore 60% more health"); }
        @Override public void onEquip(HeroContext hero) { hero.addPotionBonusPct(60); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addPotionBonusPct(-60); }
    }

    private static final class GoldIdol extends BaseRelic
    {
        private GoldIdol() { super("Gold Idol", "Earn 30% more gold"); }
        @Override public void onEquip(HeroContext hero) { hero.addGoldBonusPct(30); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addGoldBonusPct(-30); }
    }

    private static final class BattleStandard extends BaseRelic
    {
        private BattleStandard() { super("Battle Standard", "Start every battle with 30 shield"); }
        @Override public void onBattleStart(HeroContext hero, Combatant enemy)
        {
            hero.addShield(30);
            System.out.println(abyss.ui.Language.battle("[Battle Standard]: You begin with 30 extra shield."));
        }
    }

    private static final class ChronoCharm extends BaseRelic
    {
        private ChronoCharm() { super("Chrono Charm", "Skill cooldowns reduced by 1 turn"); }
        @Override public void onEquip(HeroContext hero) { hero.addCooldownReduction(1); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.addCooldownReduction(-1); }
    }

    private static final class PhoenixFeather extends BaseRelic
    {
        private PhoenixFeather() { super("Phoenix Feather", "Revive once per battle at 40% health"); }
        @Override public void onEquip(HeroContext hero) { hero.enablePhoenix(); }
        @Override public void onUnequip(abyss.combat.Hero hero) { hero.disablePhoenix(); }
    }

    private static final class BerserkerMask extends BaseRelic
    {
        private BerserkerMask() { super("Berserker Mask", "Deal 12% more damage"); }
        @Override public int modifyOutgoingDamage(HeroContext hero, Combatant enemy, int damage)
        {
            return damage * 112 / 100;
        }
    }

    private static final class SerpentRing extends BaseRelic
    {
        private SerpentRing() { super("Serpent Ring", "Basic attacks inflict Poison"); }
        @Override public void onBasicAttack(HeroContext hero, Combatant enemy)
        {
            if (enemy.isAlive())
            {
                enemy.applyStatus("Poison", 3, 4, true);
                System.out.println(abyss.ui.Language.battle("[Serpent Ring] venom seeps into the wound (Poison 3 turns)."));
            }
        }
    }
}

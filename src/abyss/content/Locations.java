package abyss.content;

import abyss.combat.Hero;

import java.util.ArrayList;
import java.util.List;

/** Campfire and merchant location effects. */
public final class Locations
{
    private Locations() { }

    public static void visitCamp(LocationServices input, Hero hero)
    {
        System.out.println(abyss.ui.Language.t("Campfire: 1. Rest (restore 35% health)  2. Train (lose 12 health, gain 2 attack)")
                + abyss.ui.Language.t("  3. Investigate the embers  4. Leave"));
        int choice = input.readChoice(1, 4);
        if (choice == 1) { int heal = (int) (hero.getMaxHealth() * 0.35); hero.restoreHealth(heal); System.out.println(abyss.ui.Language.t("You rest by the fire and restore ") + heal + abyss.ui.Language.t(" health.")); }
        else if (choice == 2) {
            if (hero.spendHealth(12)) { hero.addAttack(2); System.out.println(abyss.ui.Language.t("Training complete. Attack +2.")); }
            else System.out.println(abyss.ui.Language.t("Not enough health to train safely."));
        }
        else if (choice == 3) resolveCampEvent(input, hero);
        else System.out.println(abyss.ui.Language.t("You leave the crackling fire behind."));
    }

    public static void visitShop(LocationServices input, Hero hero)
    {
        List<ShopItem> stock = drawStock(input);
        while (true)
        {
            System.out.println(abyss.ui.Language.t("Merchant stock:"));
            for (int index = 0; index < stock.size(); index++)
            {
                ShopItem item = stock.get(index);
                System.out.println("  " + (index + 1) + ". " + abyss.ui.Language.t(item.label) + " (" + item.cost + abyss.ui.Language.t(" gold, ") + item.description(hero) + ")");
            }
            int leave = stock.size() + 1;
            System.out.println("  " + leave + abyss.ui.Language.t(". Leave"));
            int choice = input.readChoice(1, leave);
            if (choice == leave) return;
            ShopItem item = stock.get(choice - 1);
            if (hero.getGold() < item.cost) { System.out.println(abyss.ui.Language.t("Not enough gold.\n")); continue; }
            hero.spendGold(item.cost);
            item.apply(hero);
            stock.remove(item);
            if (stock.isEmpty()) { System.out.println(abyss.ui.Language.t("The merchant has sold out.")); return; }
        }
    }

    private static void resolveCampEvent(LocationServices input, Hero hero)
    {
        int roll = input.nextInt(100);
        if (roll < 45)
        {
            hero.restoreHealth(22);
            hero.addShield(15);
            System.out.println(abyss.ui.Language.t("A fire spirit blesses the camp. Restore 22 health and gain 15 shield."));
        }
        else if (roll < 80)
        {
            if (hero.spendHealth(10)) {
                hero.addAttack(3);
                System.out.println(abyss.ui.Language.t("The embers test your resolve. Lose 10 health, gain 3 attack."));
            } else System.out.println(abyss.ui.Language.t("The embers spare your weakened body. Nothing happens."));
        }
        else
        {
            hero.receivePotion();
            hero.addDefense(1);
            System.out.println(abyss.ui.Language.t("You find an abandoned pack: gain a potion and 1 defense."));
        }
    }

    private static List<ShopItem> drawStock(LocationServices input)
    {
        List<ShopItem> remaining = new ArrayList<>(List.of(ShopItem.values()));
        List<ShopItem> stock = new ArrayList<>();
        while (stock.size() < 3)
        {
            stock.add(remaining.remove(input.nextInt(remaining.size())));
        }
        return stock;
    }

    private enum ShopItem
    {
        POTION("Potion", 18, "gain 1 potion") { @Override void apply(Hero hero) { hero.receivePotion(); System.out.println(abyss.ui.Language.t("You received a potion.")); } },
        WHETSTONE("Whetstone", 32, "attack +2") { @Override void apply(Hero hero) {
            int gain = highDifficultyStatGain(hero); hero.addAttack(gain);
            System.out.println(statMessage(true, gain));
        } @Override String description(Hero hero) { return statDescription(true, hero); } },
        CHAINMAIL("Chainmail", 35, "defense +2") { @Override void apply(Hero hero) {
            int gain = highDifficultyStatGain(hero); hero.addDefense(gain);
            System.out.println(statMessage(false, gain));
        } @Override String description(Hero hero) { return statDescription(false, hero); } },
        BARRIER("Barrier", 24, "gain 25 shield") { @Override void apply(Hero hero) { hero.addShield(25); System.out.println(abyss.ui.Language.t("A magical barrier grants 25 shield.")); } },
        VITALITY_TONIC("Vitality Tonic", 38, "max health +12; restore 20") { @Override void apply(Hero hero) { hero.addMaxHealth(12); hero.restoreHealth(20); System.out.println(abyss.ui.Language.t("Vitality flows through you. Max health +12 and restore 20 health.")); } },
        SMOKE_BOMB("Smoke Bomb", 28, "gain 1 potion and 12 shield") { @Override void apply(Hero hero) { hero.receivePotion(); hero.addShield(12); System.out.println(abyss.ui.Language.t("You pack a smoke bomb: gain a potion and 12 shield.")); } };

        private final String label;
        private final int cost;
        private final String description;

        ShopItem(String label, int cost, String description)
        {
            this.label = label;
            this.cost = cost;
            this.description = description;
        }

        abstract void apply(Hero hero);

        String description(Hero hero) { return abyss.ui.Language.t(description); }

        private static int highDifficultyStatGain(Hero hero) {
            return switch (hero.getDifficulty()) {
                case NIGHTMARE -> 4;
                case ULTRA_NIGHTMARE -> 6;
                default -> 2;
            };
        }

        private static String statDescription(boolean attack, Hero hero) {
            int gain = highDifficultyStatGain(hero);
            return abyss.ui.Language.isChinese() ? (attack ? "攻击 +" : "防御 +") + gain
                    : (attack ? "attack +" : "defense +") + gain;
        }

        private static String statMessage(boolean attack, int gain) {
            if (abyss.ui.Language.isChinese()) return attack
                    ? "你的武器更加锋利。攻击 +" + gain + "。"
                    : "你的护甲得到强化。防御 +" + gain + "。";
            return attack ? "Your weapon is sharper. Attack +" + gain + "."
                    : "Your armor is reinforced. Defense +" + gain + ".";
        }
    }
}

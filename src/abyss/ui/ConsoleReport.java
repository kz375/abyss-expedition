package abyss.ui;

import abyss.combat.Hero;
import abyss.core.RunStatistics;

/** Expedition end-screen rendering and score calculation. */
public final class ConsoleReport
{
    private ConsoleReport() { }

    public static void print(Hero hero, RunStatistics s, boolean victory, int finalFloor)
    {
        ConsolePresentation.section(Language.t("EXPEDITION RECORD"));
        System.out.println(dotted(Language.t("Floors reached"), s.getFloorReached() + " / " + finalFloor));
        System.out.println(dotted(Language.t("Enemies slain"), (s.getNormalKills() + s.getEliteKills() + s.getBossKills()) + Language.t(" (Elite ") + s.getEliteKills() + Language.t(", Boss ") + s.getBossKills() + (Language.isChinese() ? "）" : ")")));
        System.out.println(dotted(Language.t("Total damage dealt"), s.getDamageDealt()));
        System.out.println(dotted(Language.t("Total damage taken"), s.getDamageTaken()));
        System.out.println(dotted(Language.t("Gold earned"), s.getGoldEarned()));
        System.out.println(dotted(Language.t("Potions drunk"), s.getPotionsDrunk()));
        System.out.println(dotted(Language.t("Events resolved"), s.getEventsResolved()));
        System.out.println(dotted(Language.t("Battle rounds"), s.getTotalTurns()));
        System.out.println(dotted(Language.t("Relics collected"), hero.getRelicCount()));
        System.out.println("  ────────────────────────────────────────────────────────────");
        int score = s.getNormalKills() * 15 + s.getEliteKills() * 40 + s.getBossKills() * 100
                + s.getDamageDealt() / 15 + s.getEventsResolved() * 10 + s.getGoldEarned() / 2
                + s.getFloorReached() * 60 + (victory ? 600 : 0);
        String rank = score >= 1600 ? "S" : score >= 1200 ? "A" : score >= 800 ? "B" : score >= 450 ? "C" : "D";
        System.out.println(Language.t("  RANK  ") + rank + Language.t("                         SCORE  ") + score);
        System.out.println("  " + (victory ? Language.t("The abyss remembers your triumph.") : hero.isAlive()
                ? Language.t("This is an interim report, not a completed expedition.") : Language.t("Your journey ends. Your achievements endure.")));
        System.out.println("  ────────────────────────────────────────────────────────────");
        System.out.println(dotted(Language.t("Health remaining"), hero.getHealth() + " / " + hero.getMaxHealth()));
        System.out.println(dotted(Language.t("Gold carried"), hero.getGold()));
        if (hero.getRelicCount() > 0) {
            System.out.println(Language.t("\n  RELIQUARY"));
            for (var relic : hero.getEquippedRelics()) System.out.println("    ❉ " + Language.t(relic.getName()));
        }
    }

    private static String dotted(String label, Object value)
    {
        return "  " + ConsoleLayout.padded(label, 24) + "  " + value;
    }
}

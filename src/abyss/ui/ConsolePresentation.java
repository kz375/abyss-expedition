package abyss.ui;

import abyss.combat.Hero;
import abyss.core.RunStatistics;

/** Shared terminal styling without nested frames or ANSI dependencies. */
public final class ConsolePresentation {
    private ConsolePresentation() { }
    public static void section(String title) {
        System.out.println("\n  ────────────────────────────────────────────────────────────");
        System.out.println("  " + title);
        System.out.println("  ────────────────────────────────────────────────────────────");
    }
    public static void printTitle() {
        System.out.println("\n\n                         ░  ▒  ▓  ❉  ▓  ▒  ░");
        System.out.println(Language.t("\n                  A B Y S S   E X P E D I T I O N"));
        System.out.println(Language.t("                     An adventure of risk and reward"));
        System.out.println(Language.t("\n                Eight floors below. One relic to reclaim."));
        System.out.println(Language.t("                 Every descent writes another legend."));
        System.out.println("\n                         " + Language.t("Version ") + abyss.config.GameVersion.NUMBER);
        System.out.println("                         " + Language.t(abyss.config.GameVersion.NOTE));
    }
    public static void printEnding(Hero hero, boolean gameWon, RunStatistics statistics, int finalFloor) {
        boolean victory = hero.isAlive() && gameWon;
        section(victory ? Language.t("❉  VICTORY  /  THE STAR RELIC RECLAIMED")
                : !hero.isAlive() ? Language.t("☠  DEFEAT  /  A LEGEND REMEMBERED") : Language.t("░  EXPEDITION PAUSED"));
        System.out.println("\n  " + (victory ? Language.t("You claim the Star Relic and become a new legend!")
                : !hero.isAlive() ? Language.t("You fell in the abyss, but courage never dies.")
                : Language.t("The abyss will wait. Resume from your last checkpoint.")));
        System.out.println("\n  " + hero.getName() + "  /  " + Language.t(hero.getHeroClass().getName()) + Language.t("  /  Level ") + hero.getLevel());
        ConsoleReport.print(hero, statistics, victory, finalFloor);
        if (victory || !hero.isAlive()) System.out.println("\n  " + Language.t("Thank you for venturing into the abyss."));
    }
}

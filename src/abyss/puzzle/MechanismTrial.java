package abyss.puzzle;

import abyss.combat.Hero;
import abyss.content.*;
import abyss.core.RunStatistics;
import abyss.ui.ConsoleInput;
import java.util.List;
import java.util.Random;

/** Narrative and settlement for the expedition encounter, separate from the standalone puzzle. */
public final class MechanismTrial {
    private MechanismTrial() { }
    public static void introduction(ConsoleInput input) {
        System.out.println(abyss.ui.Language.t("\nThe torches flicker. A low rumble rises beneath your feet."));
        System.out.println(abyss.ui.Language.t("A hooded figure emerges from the darkness."));
        System.out.println(abyss.ui.Language.t("\"Strength will not save you here. Only your next move matters.\""));
        System.out.println(abyss.ui.Language.t("The floor fractures into tiles. Two golden cores awaken."));
        System.out.println(abyss.ui.Language.t("The Trial of the Twin Cores begins."));
        System.out.print(abyss.ui.Language.t("\nPress Enter to enter the trial..."));
        input.nextLine();
    }
    public static void settle(PuzzleEncounter.Result result, Hero hero, Random random, RunStatistics statistics, ConsoleInput input) {
        if (result == PuzzleEncounter.Result.UNAVAILABLE) {
            System.out.println(abyss.ui.Language.t("The trial could not open. You return safely without a penalty."));
            return;
        }
        if (result == PuzzleEncounter.Result.VICTORY) {
            System.out.println(abyss.ui.Language.t("\nThe cores shatter. The guardian crumbles, and the path back opens."));
            int bonus = hero.getGold() / 50; // Whole coins; 2% of the balance before other rewards.
            hero.addGold(bonus); statistics.addGoldEarned(bonus);
            List<RelicEffect> relics = RelicCatalog.all().stream().filter(r -> !hero.ownsRelic(r.getName())).toList();
            if (relics.isEmpty()) {
                hero.addGold(40); statistics.addGoldEarned(40);
                System.out.println(abyss.ui.Language.t("All relics are already yours. You receive 40 compensation gold."));
            } else {
                System.out.println(abyss.ui.Language.t("Choose one relic from the entire collection:"));
                for (int i=0; i<relics.size(); i++) System.out.println("  " + (i+1) + ". " + abyss.ui.Language.t(relics.get(i).getName()) + " - " + abyss.ui.Language.t(relics.get(i).getDescription()));
                hero.equipRelic(relics.get(input.readChoice(1, relics.size())-1));
            }
            hero.healToFull();
            System.out.println(abyss.ui.Language.t("Health fully restored. Trial gold bonus: ") + bonus + ".");
        } else {
            System.out.println(abyss.ui.Language.t("\nThe guardian seals your fate. You are cast back into the abyss, stripped of part of your fortune."));
            int before = hero.getGold();
            RelicEffect lost = hero.loseRandomRelic(random);
            hero.applyMechanismFailure();
            System.out.println(abyss.ui.Language.t("Trial penalty settled. Health: ") + hero.getHealth() + "/" + hero.getMaxHealth()
                    + abyss.ui.Language.t("; gold lost: ") + (before-hero.getGold()) + abyss.ui.Language.t("; relic lost: ") + (lost == null ? abyss.ui.Language.t("none") : abyss.ui.Language.t(lost.getName())) + ".");
        }
    }
}

package abyss.ui;

import abyss.combat.Hero;
import abyss.combat.StatusEffect;
import abyss.content.RelicEffect;

import java.util.ArrayList;
import java.util.List;

/** Rendering for a hero status block. */
public final class ConsoleStatus
{
    private ConsoleStatus() { }

    public static void printHero(Hero hero, int barLength)
    {
        System.out.println("\n  [" + hero.getName() + " - " + abyss.ui.Language.t(hero.getHeroClass().getName()) + abyss.ui.Language.t(" - Level ") + hero.getLevel() + "]");
        System.out.println(abyss.ui.Language.t("  Health ") + bar(hero.getHealth(), hero.getMaxHealth(), barLength));
        System.out.println(abyss.ui.Language.t("  Attack ") + hero.getAttack() + abyss.ui.Language.t("   Defense ") + hero.getDefense() + abyss.ui.Language.t("   Shield ") + hero.getShield());
        System.out.println(abyss.ui.Language.t("  Gold ") + hero.getGold() + abyss.ui.Language.t("   Potions ") + hero.getPotions() + abyss.ui.Language.t("   Skill CD ") + hero.getSkillCooldown());
        System.out.println(abyss.ui.Language.t("  Status: ") + (hero.getStatuses().isEmpty() ? abyss.ui.Language.t("None") : describe(hero.getStatuses())));
        List<String> relicNames = new ArrayList<>();
        for (RelicEffect relic : hero.getEquippedRelics()) relicNames.add(abyss.ui.Language.t(relic.getName()));
        System.out.println(abyss.ui.Language.t("  Relics: ") + (relicNames.isEmpty() ? abyss.ui.Language.t("None") : String.join(", ", relicNames)));
    }

    private static String bar(int current, int maximum, int length)
    {
        int filled = Math.min(length, (int) Math.round((double) Math.max(0, current) / Math.max(1, maximum) * length));
        return "[" + "#".repeat(filled) + ".".repeat(length - filled) + "] " + current + "/" + maximum;
    }

    private static String describe(List<StatusEffect> statuses)
    {
        List<String> parts = new ArrayList<>();
        for (StatusEffect status : statuses)
            parts.add(ConsoleBattle.statusDescription(status));
        return String.join(", ", parts);
    }
}

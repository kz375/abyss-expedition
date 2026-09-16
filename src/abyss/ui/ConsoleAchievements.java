package abyss.ui;

import abyss.achievement.*;

public final class ConsoleAchievements {
    private ConsoleAchievements() { }
    public static void print(AchievementBook book, boolean summary) {
        ConsolePresentation.section(Language.t("CHRONICLE OF THE ABYSS"));
        System.out.println(Language.t("  Achievements  ") + book.unlocked().size() + " / " + Achievement.values().length);
        System.out.println(Language.t("  Permanent milestones. No combat bonuses.\n"));
        for (Achievement a : Achievement.values()) {
            if (summary && !book.recent().contains(a)) continue;
            System.out.println("  " + (book.unlocked().contains(a) ? "[+] " : "[ ] ") + Language.t(a.title())
                    + (book.recent().contains(a) ? Language.t("  NEW") : ""));
            if (!summary) System.out.println("      " + Language.t(a.description()));
        }
        if (summary && book.recent().isEmpty()) System.out.println(Language.t("  No new milestones this session. Your next legend awaits."));
        System.out.println();
    }
}

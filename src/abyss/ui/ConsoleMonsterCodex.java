package abyss.ui;

import abyss.achievement.AchievementBook;
import abyss.content.MonsterType;

/** Permanent roster view. The codex contains every monster; no enemy is hidden by random generation. */
public final class ConsoleMonsterCodex {
    private ConsoleMonsterCodex() { }
    public static void print(AchievementBook book) {
        ConsolePresentation.section(Language.t("MONSTER CODEX"));
        System.out.println(Language.t("  Defeated monsters  ") + book.defeatedMonsters().size() + " / " + MonsterType.values().length);
        System.out.println(Language.t("  Every creature is listed. Defeat it once to complete its entry.\n"));
        for (MonsterType monster : MonsterType.values()) {
            boolean defeated = book.hasDefeatedMonster(monster.monsterName());
            int kills = book.monsterKillCount(monster.monsterName());
            System.out.println("  " + (defeated ? "[+] " : "[ ] ") + Language.t(monster.monsterName())
                    + "  ·  " + Language.t(monster.category()) + "  ·  " + Language.t(monster.behavior().getSkillName())
                    + (defeated ? Language.t("  Defeated ") + kills + Language.t(" times") : Language.t("  Not defeated")));
        }
        System.out.println();
    }
}

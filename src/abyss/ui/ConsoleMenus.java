package abyss.ui;
import abyss.config.Difficulty;
import abyss.content.HeroClass;
import abyss.core.GameInterruptedException;

public final class ConsoleMenus {
    private ConsoleMenus() { }
    public static int home(ConsoleInput input, abyss.save.SaveData save, abyss.achievement.AchievementBook book) {
        while (true) {
            ConsolePresentation.section(Language.t("THE GATEWAY"));
            if (save != null) {
                System.out.println(Language.t("  CHECKPOINT  /  ") + save.getHeroName() + Language.t("  /  Floor ") + save.getFloor());
                System.out.println("  " + Language.t(save.getDifficulty().label()) + "  ·  " + Language.t(save.getHeroClassName()) + "\n");
                if (save.hasSeed()) System.out.println(Language.t("  SEED  /  ") + save.getSeed() + "\n");
            } else System.out.println(Language.t("  No active expedition. A new path awaits.\n"));
            System.out.println("  [1]  " + (save == null ? Language.t("Begin expedition") : Language.t("Continue expedition")));
            System.out.println(Language.t("  [2]  Load save / enter seed  / LIST to browse archives"));
            int achievementChoice = 3;
            int codexChoice = 4;
            int exitChoice = 5;
            System.out.println("  [" + achievementChoice + Language.t("]  Achievements         / ") + book.unlocked().size() + Language.t(" of ")
                    + abyss.achievement.Achievement.values().length + Language.t(" unlocked"));
            System.out.println("  [" + codexChoice + "]  " + Language.t("Monster Codex"));
            System.out.println("  [" + exitChoice + Language.t("]  Leave the abyss\n"));
            int languageChoice = exitChoice + 1;
            System.out.println("  [" + languageChoice + "]  " + Language.t("Language") + "  ·  " + (Language.isChinese() ? "中文" : "English"));
            System.out.println("\n" + Language.t("  Type a number, then press Enter."));
            int choice = input.readChoice(1,languageChoice);
            if (choice == languageChoice) {
                ConsolePresentation.section(Language.t("Language"));
                // Keep the Chinese language name in Chinese in both interfaces.
                System.out.println("  [1] English\n  [2] 简体中文");
                Language.select(input.readChoice(1,2) == 2);
                ConsolePresentation.printTitle();
                continue;
            }
            if (choice == exitChoice) return 4;
            if (choice != achievementChoice && choice != codexChoice) return choice;
            if (choice == achievementChoice) ConsoleAchievements.print(book, false);
            else ConsoleMonsterCodex.print(book);
            System.out.print(Language.t("  Press Enter to return to the gateway..."));
            input.nextLine();
        }
    }
    public static Difficulty chooseDifficulty(ConsoleInput INPUT)
    {
        ConsolePresentation.section(Language.t("01  /  CHOOSE YOUR DESCENT"));
        String[] descriptions = {Language.t("Discover the abyss at a gentler pace."), Language.t("The standard expedition."),
                Language.t("A harsher journey. Plan every encounter."), Language.t("The deepest challenge. Prepare to adapt.")};
        Difficulty[] difficulties = Difficulty.values();
        for (int index = 0; index < difficulties.length; index++)
        {
            System.out.println("  [" + (index + 1) + "]  " + Language.t(difficulties[index].label()));
            System.out.println("       " + descriptions[index] + "\n");
        }
        return difficulties[INPUT.readChoice(1, difficulties.length) - 1];
    }
    public static HeroClass chooseClass(ConsoleInput INPUT, Difficulty difficulty)
    {
        HeroClass[] visibleClasses = {
                HeroClass.WARRIOR, HeroClass.MAGE, HeroClass.RANGER,
                HeroClass.PALADIN, HeroClass.NECROMANCER};
        HeroClass[] revealedClasses = {HeroClass.CREATOR};
        boolean creatorRevealed = false;
        double multiplier = difficulty.heroStatMultiplier();
        while (true)
        {
            HeroClass[] classes = creatorRevealed ? revealedClasses : visibleClasses;
            ConsolePresentation.section(creatorRevealed ? Language.t("02  /  THE HIDDEN PATH") : Language.t("02  /  CHOOSE YOUR CHAMPION"));
            for (int index = 0; index < classes.length; index++)
            {
                HeroClass heroClass = classes[index];
                int displayNumber = creatorRevealed ? 6 : index + 1;
                System.out.println("  [" + displayNumber + "] " + ConsoleLayout.padded(Language.t(heroClass.getName()), 14)
                        + Language.t("HP ") + String.format("%-4d", (int) (heroClass.getBaseHealth() * multiplier))
                        + Language.t(" ATK ") + String.format("%-3d", (int) (heroClass.getBaseAttack() * multiplier))
                        + Language.t(" DEF ") + (int) (heroClass.getBaseDefense() * multiplier));
                System.out.println("      ⚔ " + Language.t(heroClass.getSkillName()) + "  ·  " + Language.t(heroClass.getSkillDescription()));
                System.out.println("      ✦ " + Language.t(heroClass.getPassiveSkillName()) + "  ·  " + Language.t(heroClass.getPassiveSkillDescription()) + "\n");
            }

            if (!INPUT.hasNextLine())
            {
                throw new GameInterruptedException();
            }
            System.out.print("> ");
            String text = INPUT.nextLine().trim();
            if ("kz".equalsIgnoreCase(text))
            {
                creatorRevealed = true;
                System.out.println(Language.t("\n❉ A hidden path opens. Creator has been revealed.\n"));
                continue;
            }
            try
            {
                int choice = Integer.parseInt(text);
                if (creatorRevealed && choice == 6)
                {
                    return HeroClass.CREATOR;
                }
                if (!creatorRevealed && choice >= 1 && choice <= classes.length)
                {
                    return classes[choice - 1];
                }
            }
            catch (NumberFormatException ignored)
            {
                // KZ is the only non-numeric class-selection input.
            }
            System.out.println(creatorRevealed
                    ? Language.t("Enter 6 to choose Creator.")
                    : Language.t("Enter a number between 1 and ") + classes.length + ".");
        }
    }
}

package abyss.ui;

import abyss.save.*;

/** A seed selects a retained expedition, not merely a fresh random world. */
public final class ConsoleSeeds {
    public record Selection(long seed, SaveData checkpoint) { }
    private ConsoleSeeds() { }
    public static Selection choose(ConsoleInput input) {
        ConsolePresentation.section(Language.t("EXPEDITION ARCHIVES"));
        System.out.println(Language.t("  Enter an existing seed to restore its saved progress."));
        System.out.println(Language.t("  New number: create expedition. Blank: generate a seed."));
        System.out.println(Language.t("  Type LIST to view retained expeditions."));
        while (true) {
            System.out.print(Language.t("Seed > "));
            String text = input.nextLine().trim();
            if (text.equalsIgnoreCase("list") || text.equals("列表")) {
                var saves = SaveManager.archives();
                if (saves.isEmpty()) System.out.println(Language.t("  No retained expeditions yet."));
                for (SaveData save : saves) System.out.println("  " + save.getSeed() + "  /  " + save.getHeroName()
                        + Language.t("  /  Floor ") + save.getFloor() + "  /  " + Language.t(save.getDifficulty().label())
                        + "  /  " + (save.getOutcome() == 0 ? Language.t("IN PROGRESS") : save.getOutcome() == 1 ? Language.t("VICTORY") : Language.t("DEFEAT")));
                continue;
            }
            try {
                long seed = text.isEmpty() ? SaveManager.unusedSeed() : Long.parseLong(text);
                if (SaveManager.seedExists(seed)) {
                    var saved = SaveManager.loadSeed(seed);
                    if (saved.isEmpty()) { System.out.println(Language.t("Cannot restore this archive; it has NOT been replaced. Choose another seed.")); continue; }
                    return new Selection(seed, saved.get());
                }
                System.out.println(Language.t("  New expedition seed: ") + seed);
                return new Selection(seed, null);
            } catch (NumberFormatException e) {
                System.out.println(Language.t("Enter a whole number from -9223372036854775808 to 9223372036854775807, or LIST."));
            }
        }
    }
}

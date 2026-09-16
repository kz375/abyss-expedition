package abyss.content;

import abyss.combat.*;
import abyss.core.*;
import abyss.ui.ConsoleInput;
import java.util.*;
import java.util.function.Supplier;

/** Gold, experience, relic offers and kill statistics for the main expedition. */
public final class ExpeditionRewards {
    private final ConsoleInput input;
    private final RunStatistics stats;
    private final Supplier<Random> random;
    public ExpeditionRewards(ConsoleInput input, RunStatistics stats, Supplier<Random> random) {
        this.input = input; this.stats = stats; this.random = random;
    }
    public List<RelicEffect> unownedRelics(Hero hero) {
        return RelicCatalog.all().stream().filter(r -> !hero.ownsRelic(r.getName())).toList();
    }
    public int gainGold(Hero hero, int amount) {
        int total = amount * (100 + hero.getGoldBonusPct()) / 100;
        hero.addGold(total); stats.addGoldEarned(total); return total;
    }
    public void grantRelic(Hero hero) {
        List<RelicEffect> offers = unownedRelics(hero);
        if (offers.isEmpty()) {
            System.out.println(abyss.ui.Language.battle("No relics remain in the abyss. You receive ") + gainGold(hero,40) + abyss.ui.Language.battle(" gold instead."));
            return;
        }
        offers = RandomSelection.sampleDistinct(offers,3,random.get());
        System.out.println(abyss.ui.Language.battle("\nChoose one relic reward:"));
        for (int i=0;i<offers.size();i++) System.out.println("  " + (i+1) + ". "
                + abyss.ui.ConsoleLayout.padded(abyss.ui.Language.t(offers.get(i).getName()),16) + " - " + abyss.ui.Language.t(offers.get(i).getDescription()));
        hero.equipRelic(offers.get(input.readChoice(1,offers.size())-1));
    }
    public void victory(Hero hero, Enemy enemy) {
        if (enemy.isBoss()) stats.addBossKill();
        else if (enemy.isElite()) stats.addEliteKill();
        else stats.addNormalKill();
        System.out.println(abyss.ui.Language.battle("\nDefeated ") + abyss.ui.Language.t(enemy.getName()) + abyss.ui.Language.battle("! Gained ") + gainGold(hero,enemy.getGoldReward()) + abyss.ui.Language.battle(" gold."));
        hero.gainExperience(enemy.getExperienceReward());
    }
}

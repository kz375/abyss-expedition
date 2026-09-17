package abyss.ui;
import java.util.*;
import abyss.combat.*;

public final class ConsoleBattle {
    private static final int BAR_LENGTH = 22;
    private ConsoleBattle() { }
    private static String bar(int current, int maximum)
    {
        if (maximum <= 0)
        {
            maximum = 1;
        }
        int filled = (int) Math.round((double) Math.max(0, current) / maximum * BAR_LENGTH);
        if (filled > BAR_LENGTH)
        {
            filled = BAR_LENGTH;
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < filled; i++)
        {
            builder.append('#');
        }
        for (int i = filled; i < BAR_LENGTH; i++)
        {
            builder.append('.');
        }
        return builder.append("] ").append(current).append("/").append(maximum).toString();
    }
    private static String describeStatuses(List<abyss.combat.StatusEffect> statuses)
    {
        if (statuses.isEmpty())
        {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (abyss.combat.StatusEffect status : statuses)
        {
            parts.add(statusDescription(status));
        }
        return String.join(", ", parts);
    }
    public static void printBattleState(Hero hero, Enemy enemy)
    {
        System.out.println("\n  ──────────────────────────── ☠ ────────────────────────────");
        printCombatant(Language.t("You"), hero);
        printCombatant(Language.t(enemy.getName()), enemy);
        System.out.println("  ────────────────────────────────────────────────────────────");
    }
    public static void printBattleState(Hero hero, Enemy enemy, List<Enemy> minions)
    {
        printBattleState(hero, enemy);
        if (!minions.isEmpty()) {
            System.out.println(Language.isChinese() ? "  召唤物：" : "  Summoned foes:");
            for (int index = 0; index < minions.size(); index++) {
                System.out.print("  " + (index + 1) + ". ");
                printCombatant(Language.t(minions.get(index).getName()), minions.get(index));
            }
            System.out.println("  ────────────────────────────────────────────────────────────");
        }
    }
    private static void printCombatant(String label, abyss.combat.Combatant combatant)
    {
        System.out.println("  " + ConsoleLayout.padded(label, 20) + bar(combatant.getHealth(), combatant.getMaxHealth())
                + (combatant.getShield() > 0 ? (Language.isChinese() ? "  护盾 " : "  Shield ") + combatant.getShield() : ""));
        String statusText = describeStatuses(combatant.getStatuses());
        if (!statusText.isEmpty())
        {
            System.out.println("  " + ConsoleLayout.padded(Language.t("Status"), 20) + statusText);
        }
        if (!combatant.isPlayer())
        {
            Enemy enemy = (Enemy) combatant;
            if (enemy.isElite() && !enemy.getAffix().label().isEmpty())
            {
                System.out.println("  " + ConsoleLayout.padded(Language.t("Elite Affix"), 20) + Language.t(enemy.getAffix().label()));
            }
            if (enemy.isBoss() && enemy.isPhaseTwo())
            {
                System.out.println("  " + ConsoleLayout.padded(Language.t("Boss Phase"), 20) + (enemy.isPhaseThree() ? "III" : "II"));
            }
            if (enemy.isProtectedBySummons())
            {
                System.out.println("  " + ConsoleLayout.padded(Language.isChinese() ? "深渊护盾" : "Abyssal Shield", 20)
                        + (Language.isChinese() ? "先击败召唤物才能伤害首领" : "Defeat the summon to damage the boss"));
            }
            String intentText = enemy.getIntent() == EnemyIntent.SPECIAL
                    ? Language.t(enemy.getIntent().label()) + (Language.isChinese() ? "：" : ": ") + Language.t(enemy.getBehavior().getSkillName())
                    : Language.t(enemy.getIntent().label());
            System.out.println("  " + ConsoleLayout.padded(Language.t("Intent"), 20) + intentText);
        }
    }

    public static String statusDescription(StatusEffect status) {
        if (Language.isChinese()) return Language.t(status.getName()) + "（剩余 " + status.getTurns() + " 回合"
                + (status.getPower() > 0 ? "，强度 " + status.getPower() : "") + "）";
        return status.getName() + "(" + status.getTurns() + "t" + (status.getPower() > 0 ? "," + status.getPower() : "") + ")";
    }
    public static void printActions(Hero hero, Enemy enemy) {
        printActions(hero, enemy, List.of());
    }
    public static void printActions(Hero hero, Enemy enemy, List<Enemy> minions) {
        String skill = Language.t(hero.getSkill().getName());
        if (Language.isChinese()) {
            String cooldown = hero.getSkillCooldown() > 0 ? "（冷却 " + hero.getSkillCooldown() + " 回合）" : "（可施放）";
            System.out.println("  [1] " + (enemy.isProtectedBySummons() ? "攻击召唤物护盾" : "普通攻击"));
            System.out.println("  [2] " + skill + cooldown);
            System.out.println("  [3] 使用药水（剩余 " + hero.getPotions() + " 瓶）");
            System.out.println("  [4] 查看状态" + (enemy.isBoss() ? "" : "    [5] 撤退"));
        } else {
            String label = skill + (hero.getSkillCooldown() > 0 ? " (CD:" + hero.getSkillCooldown() + ")" : "");
            System.out.println("  1. " + (enemy.isProtectedBySummons() ? "Attack ward summon" : "Attack") + "          2. " + label);
            System.out.println("  3. Drink Potion (" + hero.getPotions() + ")    4. View Status" + (enemy.isBoss() ? "" : "  5. Retreat"));
        }
        int firstMinionChoice = enemy.isBoss() ? 5 : 6;
        for (int index = 0; index < minions.size(); index++) {
            Enemy minion = minions.get(index);
            String label = Language.isChinese() ? "普通攻击 " + Language.t(minion.getName()) : "Attack " + minion.getName();
            System.out.println("  [" + (firstMinionChoice + index) + "] " + label);
        }
        System.out.println("  ────────────────────────────────────────────────────────────");
    }
}

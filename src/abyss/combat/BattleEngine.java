package abyss.combat;
import abyss.core.RunStatistics;
import abyss.content.RelicEffect;
import abyss.ui.*;
import static abyss.ui.ConsoleBattle.printBattleState;

public final class BattleEngine {
    private final java.util.function.IntBinaryOperator choice;
    private final RunStatistics statistics;
    public BattleEngine(ConsoleInput input, RunStatistics statistics) { this(input::readChoice, statistics); }
    public BattleEngine(java.util.function.IntBinaryOperator choice, RunStatistics statistics) { this.choice = choice; this.statistics = statistics; }
    public boolean battle(Hero hero, Enemy enemy)
    {
        java.util.List<Enemy> minions = new java.util.ArrayList<>();
        String rank = enemy.isBoss() ? "BOSS " : enemy.isElite() ? "ELITE " : "";
        System.out.println(Language.isChinese()
                ? "\n❉ " + (enemy.isBoss() ? "首领战" : enemy.isElite() ? "精英战" : "遭遇战") + " · "
                    + Language.t(enemy.isElite() && enemy.getName().startsWith("Elite ") ? enemy.getName().substring(6) : enemy.getName())
                : "\n>> " + rank + "ENCOUNTER: " + enemy.getName().toUpperCase() + " <<");
        int initialShield = hero.getShield();
        triggerBattleStart(hero, enemy);
        while (hero.isAlive() && enemy.isAlive())
        {
            statistics.addTurn();
            if (hero.getHeroClass().tryExecution(hero, enemy) && !enemy.isAlive())
            {
                break;
            }

            boolean stunned = heroTurnStart(hero);
            boolean usedSkill = false;
            if (hero.isAlive() && stunned)
            {
                printBattleState(hero, enemy, minions);
                System.out.println(Language.battle("You are stunned and lose your turn!"));
            }
            else if (hero.isAlive())
            {
                while (true)
                {
                printBattleState(hero, enemy, minions);
                ConsoleBattle.printActions(hero, enemy, minions);
                int baseChoices = enemy.isBoss() ? 4 : 5;
                int maxChoice = baseChoices + minions.size();
                int choice = this.choice.applyAsInt(1, maxChoice);

                if (choice == 4)
                {
                    ConsoleStatus.printHero(hero, 22);
                    continue;
                }
                if (choice == 5 && !enemy.isBoss())
                {
                    System.out.println(Language.battle("You retreat safely and keep this expedition's rewards."));
                    hero.clearStatuses();
                    hero.limitShield(initialShield);
                    return true;
                }
                if (choice == 3)
                {
                    if (hero.getPotions() <= 0)
                    {
                        System.out.println(Language.battle("You have no potions left!"));
                        continue;
                    }
                    hero.drinkPotion();
                }
                else if (choice == 2)
                {
                    if (hero.getSkillCooldown() > 0)
                    {
                        System.out.println(Language.battle("Skill is on cooldown for ") + hero.getSkillCooldown() + Language.battle(" more turn(s)."));
                        continue;
                    }
                    hero.getSkill().use(hero, enemy);
                    hero.startSkillCooldown();
                    usedSkill = true;
                }
                else
                {
                    Enemy target = choice > baseChoices ? minions.get(choice - baseChoices - 1) : enemy;
                    int damage = hero.dealDamage(target, 1.0);
                    System.out.println(Language.battle("You attack for ") + damage + Language.battle(" damage."));
                    for (RelicEffect relic : hero.getEquippedRelics())
                    {
                        relic.onBasicAttack(hero, target);
                    }
                    hero.getHeroClass().onBasicAttack(hero, target);
                    minions.removeIf(add -> !add.isAlive());
                    if (enemy.isAlive() && enemy.isUltraNightmareFinalBoss()) {
                        Enemy minion = enemy.summonUltraNightmareMinion();
                        minions.add(minion);
                        System.out.println(Language.isChinese()
                                ? "超级噩梦【深渊召唤】：深渊领主回应普通攻击，召唤了" + Language.t(minion.getName()) + "！"
                                : "Ultra Nightmare [Abyssal Summoning]: The Abyss Lord answers your basic attack and summons " + minion.getName() + "!");
                    }
                }
                break;
                }
            }

            hero.endTurn();
            if (!usedSkill) hero.decrementSkillCooldown();
            if (!hero.isAlive() || !enemy.isAlive())
            {
                break;
            }
            enemyTurn(hero, enemy);
            if (!hero.isAlive())
            {
                break;
            }
            for (Enemy minion : minions) {
                enemyTurn(hero, minion);
                if (!hero.isAlive()) break;
            }
        }
        hero.clearStatuses();
        return false;
    }
    private void triggerBattleStart(Hero hero, Enemy enemy)
    {
        hero.clearStatuses();
        hero.resetPhoenix();
        hero.getHeroClass().onBattleStart(hero, enemy);
        enemy.prepareEliteAffix();
        enemy.prepareUltraNightmare();
        enemy.prepareIntent();
        for (RelicEffect relic : hero.getEquippedRelics())
        {
            relic.onBattleStart(hero, enemy);
        }
    }
    private boolean heroTurnStart(Hero hero)
    {
        hero.getHeroClass().onTurnStart(hero);
        return hero.beginTurn();
    }
    private void enemyTurn(Hero hero, Enemy enemy)
    {
        boolean stunned = enemy.beginTurn();
        if (!enemy.isAlive())
        {
            return;
        }
        enemy.enterPhaseTwoIfNeeded();
        enemy.enterPhaseThreeIfNeeded();
        enemy.enrageIfNeeded();
        if (stunned)
        {
            System.out.println(Language.t(enemy.getName()) + Language.battle(" is stunned and cannot act!"));
        }
        else if (enemy.getIntent() == EnemyIntent.SPECIAL)
        {
            enemy.getBehavior().useSpecial(hero, enemy);
            enemy.setSkillCooldown(enemy.isBoss() ? 3 : 4);
        }
        else
        {
            int damage = hero.receiveDamageFrom(enemy, 1.0);
            System.out.println(Language.t(enemy.getName()) + Language.battle(" attacks you for ") + damage + Language.battle(" damage."));
        }
        enemy.decrementSkillCooldown();
        enemy.endTurn();
        if (enemy.isAlive())
        {
            enemy.prepareIntent();
        }
    }
}

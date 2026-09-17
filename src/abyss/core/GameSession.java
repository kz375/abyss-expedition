package abyss.core;

import abyss.combat.BattleEngine;
import abyss.ui.ConsoleMenus;
import abyss.ui.ConsolePresentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import abyss.config.Difficulty;
import abyss.content.RelicCatalog;
import abyss.content.RelicEffect;
import abyss.content.HeroClass;
import abyss.combat.Enemy;
import abyss.combat.Hero;
import abyss.core.RunStatistics;
import abyss.core.RandomSelection;
import abyss.content.GameNode;
import abyss.content.GameNodes;
import abyss.content.GameServices;
import abyss.content.EventServices;
import abyss.content.GameEvent;
import abyss.content.LocationServices;
import abyss.content.Locations;
import abyss.ui.ConsoleStatus;
import abyss.ui.ConsoleInput;
import abyss.ui.ConsoleHeaders;
import abyss.core.GameInterruptedException;
import abyss.save.SaveData;
import abyss.save.SaveManager;
import abyss.puzzle.PuzzleEncounter;

public final class GameSession
{
    private final ConsoleInput INPUT = new ConsoleInput(new Scanner(System.in));
    private Random RANDOM = new Random();
    private final int FINAL_FLOOR = 8;
    private final int BAR_LENGTH = 22;
    private final java.util.function.Function<Random, PuzzleEncounter.Result> puzzlePlayer;

    public GameSession()
    {
        this(PuzzleEncounter::playResult);
    }

    /** Hosts supply a presentation boundary; trial rules and settlement stay shared. */
    public GameSession(java.util.function.Function<Random, PuzzleEncounter.Result> puzzlePlayer)
    {
        this.puzzlePlayer = java.util.Objects.requireNonNull(puzzlePlayer);
    }

    // ---------------- Run statistics ----------------
    private final RunStatistics STATS = new RunStatistics();
    private final BattleEngine battleEngine = new BattleEngine(INPUT, STATS);
    private final abyss.content.ExpeditionRewards rewards = new abyss.content.ExpeditionRewards(INPUT, STATS, () -> RANDOM);
    private boolean gameWon;
    private boolean mechanismEncounterUsed;
    private abyss.content.FloorRoutes routes;
    private long expeditionSeed;
    private final abyss.achievement.AchievementBook achievements = new abyss.achievement.AchievementBook(
            java.nio.file.Path.of(System.getProperty("abyss.saveDir", "saves")));

    // ---------------- Architecture overview ----------------
    // Combatant        : contract shared by Hero and Enemy; battle settlement is coded against it.
    // StatusEffect     : one subclass per status (Burn/Poison/Stun/Weak/Regen/Empower/Sunder/Curse)
    //                    with onTurnStart()/onTurnEnd() lifecycle hooks.
    // Skill            : one strategy class per hero class active skill, replaces the if-else chain.
    // EnemyBehavior    : one strategy class per enemy tier / boss skill package.
    // RelicEffect      : one class per relic, hooking onEquip/onBattleStart/modify*Damage/onBasicAttack.
    // GameNode         : polymorphic map nodes (Battle/Elite/Event/Camp/Shop/Boss), replaces the switch.
    // GameEvent        : enum with method bodies, one per random event.
    // Difficulty       : fully data driven multipliers (no if-else difficulty checks anywhere).

    private final List<RelicEffect> RELIC_POOL = RelicCatalog.all();

    private final GameNode FINAL_NODE = GameNodes.finalBattle();
    private final GameServices GAME_SERVICES = new GameServices()
    {
        @Override public boolean resolveBattle(Hero hero, int floor, boolean elite, Difficulty difficulty)
        {
            return GameSession.this.resolveBattle(hero, spawnEnemy(floor, elite, difficulty));
        }

        @Override public void resolveEvent(Hero hero, int floor, Difficulty difficulty)
        {
            GameSession.this.resolveEvent(hero, floor, difficulty);
        }

        @Override public void resolveEventCard(GameEvent event, Hero hero, int floor, Difficulty difficulty)
        {
            GameSession.this.resolveEventCard(event, hero, floor, difficulty);
        }

        @Override public void visitCamp(Hero hero) { GameSession.this.visitCamp(hero); }
        @Override public void visitShop(Hero hero) { GameSession.this.visitShop(hero); }
    };

    private final EventServices EVENT_SERVICES = new EventServices()
    {
        @Override public int readChoice(int minimum, int maximum) { return GameSession.this.readChoice(minimum, maximum); }
        @Override public int nextInt(int bound) { return RANDOM.nextInt(bound); }
        @Override public boolean nextBoolean() { return RANDOM.nextBoolean(); }
        @Override public int gainGold(Hero hero, int amount) { return rewards.gainGold(hero, amount); }
        @Override public List<RelicEffect> unownedRelics(Hero hero) { return rewards.unownedRelics(hero); }
        @Override public RelicEffect randomElement(List<RelicEffect> relics) { return GameSession.this.randomElement(relics); }
        @Override public boolean resolveBattle(Hero hero, int floor, boolean elite, Difficulty difficulty) {
            return GameSession.this.resolveBattle(hero, Enemy.spawnEvent(floor, elite, difficulty, RANDOM, STATS::addDamageDealt));
        }
        @Override public void resolveEvent(Hero hero, int floor, Difficulty difficulty) { GAME_SERVICES.resolveEvent(hero, floor, difficulty); }
        @Override public void visitCamp(Hero hero) { GAME_SERVICES.visitCamp(hero); }
        @Override public void visitShop(Hero hero) { GAME_SERVICES.visitShop(hero); }
        @Override public boolean canEncounterMechanism() { return !mechanismEncounterUsed; }
        @Override public void resolveMechanismEncounter(Hero hero, int floor, Difficulty difficulty) { GameSession.this.resolveMechanismEncounter(hero, floor, difficulty); }
    };

    private final LocationServices LOCATION_SERVICES = new LocationServices()
    {
        @Override public int readChoice(int minimum, int maximum) { return GameSession.this.readChoice(minimum, maximum); }
        @Override public int nextInt(int bound) { return RANDOM.nextInt(bound); }
    };

    // ---------------- Generic draw utilities (relic pool / route sampling) ----------------

    private <T> T randomElement(List<T> pool)
    {
        return RandomSelection.randomElement(pool, RANDOM);
    }

    // ---------------- UI helpers ----------------

    private void printFloorHeader(int floor)
    {
        ConsoleHeaders.floor(floor, FINAL_FLOOR);
    }

    // ---------------- Map flow ----------------
    private boolean resolveBattle(Hero hero, Enemy enemy)
    {
        boolean escaped = battleEngine.battle(hero, enemy);
        if (hero.isAlive() && !escaped)
        {
            achievements.recordMonster(enemy.getName());
            rewards.victory(hero, enemy);
            if (enemy.isFinalBoss())
            {
                gameWon = true;
            }
            if (enemy.isElite() || enemy.isBoss())
            {
                rewards.grantRelic(hero);
            }
            return true;
        }
        return false;
    }

    private void visitCamp(Hero hero)
    {
        Locations.visitCamp(LOCATION_SERVICES, hero);
    }

    private void visitShop(Hero hero)
    {
        Locations.visitShop(LOCATION_SERVICES, hero);
    }

    // ---------------- Events ----------------
    private void resolveEvent(Hero hero, int floor, Difficulty difficulty)
    {
        GameEvent.resolveRandom(EVENT_SERVICES, hero, floor, difficulty);
        STATS.addEventResolved();
    }

    private void resolveEventCard(GameEvent event, Hero hero, int floor, Difficulty difficulty)
    {
        event.resolve(EVENT_SERVICES, hero, floor, difficulty);
        STATS.addEventResolved();
    }

    private void resolveMechanismEncounter(Hero hero, int floor, Difficulty difficulty)
    {
        if (mechanismEncounterUsed) return;
        abyss.puzzle.MechanismTrial.introduction(INPUT);
        mechanismEncounterUsed = true;
        // Persist entry before opening the window: quitting cannot reroll a once-per-run encounter.
        saveCheckpoint(hero, floor, difficulty);
        PuzzleEncounter.Result result = puzzlePlayer.apply(RANDOM);
        if (result == PuzzleEncounter.Result.VICTORY) {
            achievements.unlock(abyss.achievement.Achievement.TWIN_CORES);
            achievements.save();
        }
        abyss.puzzle.MechanismTrial.settle(result, hero, RANDOM, STATS, INPUT);
        saveCheckpoint(hero, floor, difficulty);
    }

    // ---------------- Relics ----------------

    private Enemy spawnEnemy(int floor, boolean elite, Difficulty difficulty)
    {
        return Enemy.spawn(floor, elite, difficulty, FINAL_FLOOR, RANDOM,
                STATS::addDamageDealt);
    }

    private Hero restoreHero(SaveData save)
    {
        HeroClass heroClass = HeroClass.valueOf(save.getHeroClassName());
        Hero hero = abyss.combat.HeroFactory.create(save.getHeroName(), heroClass, save.getDifficulty(), RANDOM, STATS);
        List<RelicEffect> relics = new ArrayList<>();
        for (String relicName : save.getRelicNames())
        {
            for (RelicEffect relic : RELIC_POOL)
            {
                if (relic.getName().equals(relicName))
                {
                    relics.add(relic);
                    break;
                }
            }
        }
        save.restoreHero(hero, relics);
        return hero;
    }

    private boolean saveCheckpoint(Hero hero, int floor, Difficulty difficulty)
    {
        achievements.check(hero, STATS, difficulty, gameWon);
        if (SaveManager.save(SaveData.capture(floor, difficulty, hero, STATS, RANDOM, mechanismEncounterUsed, routes)
                .withIdentity(expeditionSeed, 0)))
        {
            System.out.println(abyss.ui.Language.isChinese()
                    ? "  ░ 检查点已保存：深渊第 " + floor + " 层 / 种子 " + expeditionSeed
                    : "  ░ Checkpoint saved for Abyss Floor " + floor + " / Seed " + expeditionSeed + ".");
            return true;
        }
        else
        {
            System.out.println(abyss.ui.Language.t("  ░ Unable to write checkpoint; the expedition continues."));
            return false;
        }
    }

    private void showStatus(Hero hero)
    {
        ConsoleStatus.printHero(hero, BAR_LENGTH);
    }

    // ---------------- Input ----------------
    private int readChoice(int minimum, int maximum)
    {
        return INPUT.readChoice(minimum, maximum);
    }

    // ---------------- Entry point ----------------
    public void run()
    {
        ConsolePresentation.printTitle();
        Hero hero = null;
        Difficulty difficulty = null;
        int floor = 1;
        try
        {
            SaveData save = SaveManager.load().orElse(null);
            int homeChoice = ConsoleMenus.home(INPUT, save, achievements);
            if (homeChoice == 4) {
                System.out.println(abyss.ui.Language.t("\n  The gates close softly. Your chronicle awaits your return."));
                return;
            }
            // Preserve the previous single-slot save before selecting any other expedition.
            if (save != null && !save.hasSeed()) {
                save.withIdentity(SaveManager.unusedSeed(), 0);
                if (!SaveManager.save(save)) {
                    System.out.println(abyss.ui.Language.t("Unable to archive the legacy checkpoint. No new expedition was started."));
                    return;
                }
                System.out.println(abyss.ui.Language.t("  Legacy checkpoint retained under seed ") + save.getSeed() + ".");
            }
            if (homeChoice == 2 || save == null) {
                var selection = abyss.ui.ConsoleSeeds.choose(INPUT);
                expeditionSeed = selection.seed();
                save = selection.checkpoint();
            } else expeditionSeed = save.getSeed();
            if (save != null)
            {
                    RANDOM = save.getRandom();
                    STATS.reset();
                    save.restoreStatistics(STATS);
                    difficulty = save.getDifficulty();
                    mechanismEncounterUsed = save.isMechanismEncounterUsed();
                    hero = restoreHero(save);
                    floor = save.getFloor();
                    routes = save.restoreRoutes(RANDOM);
                    gameWon = save.getOutcome() == 1;
                    if (save.getOutcome() != 0) {
                        System.out.println(abyss.ui.Language.t("\n  RETAINED EXPEDITION / Seed ") + expeditionSeed + abyss.ui.Language.t(" / Read-only final record"));
                        ConsolePresentation.printEnding(hero, gameWon, STATS, FINAL_FLOOR);
                        abyss.ui.ConsoleAchievements.print(achievements, true);
                        return;
                    }
                    System.out.println(abyss.ui.Language.t("\n░ Checkpoint restored. Returning to Abyss Floor ") + floor + ".");
            }

            if (hero == null)
            {
                RANDOM = new Random(expeditionSeed);
                STATS.reset();
                gameWon = false;
                mechanismEncounterUsed = false;
                System.out.print(abyss.ui.Language.t("Enter your adventurer name: "));
                if (!INPUT.hasNextLine())
                {
                    throw new GameInterruptedException();
                }
                String name = INPUT.nextLine().trim();
                if (name.length() > 200) { name = name.substring(0,200); System.out.println(abyss.ui.Language.t("Name shortened to 200 characters.")); }
                if (name.isEmpty())
                {
                    name = abyss.ui.Language.isChinese() ? "无名冒险者" : "Nameless Adventurer";
                }

                difficulty = ConsoleMenus.chooseDifficulty(INPUT);
                hero = abyss.combat.HeroFactory.create(name, ConsoleMenus.chooseClass(INPUT, difficulty), difficulty, RANDOM, STATS);
                System.out.println(abyss.ui.Language.t("\nWelcome, ") + hero.getName()
                        + abyss.ui.Language.t(". Descend eight floors of the abyss and claim the lost Star Relic."));
            }
            if (routes == null) routes = new abyss.content.FloorRoutes(RANDOM);
            STATS.setFloorReached(floor);
            if (!saveCheckpoint(hero, floor, difficulty)) {
                System.out.println(abyss.ui.Language.t("Cannot establish a durable checkpoint. Expedition stopped safely."));
                return;
            }
            while (floor <= FINAL_FLOOR && hero.isAlive())
            {
                STATS.setFloorReached(floor);
                printFloorHeader(floor);
                showStatus(hero);
                GameNode node = floor == FINAL_FLOOR ? FINAL_NODE : routes.choose(INPUT);
                saveCheckpoint(hero, floor, difficulty);
                boolean advanceFloor = node.resolve(GAME_SERVICES, hero, floor, difficulty);
                if (advanceFloor)
                {
                    floor++;
                    routes = floor <= FINAL_FLOOR ? new abyss.content.FloorRoutes(RANDOM) : null;
                    if (floor <= FINAL_FLOOR && hero.isAlive())
                    {
                        STATS.setFloorReached(floor);
                        saveCheckpoint(hero, floor, difficulty);
                    }
                }
                else if (hero.isAlive())
                {
                    saveCheckpoint(hero, floor, difficulty);
                    System.out.println(abyss.ui.Language.t("You remain on Abyss Floor ") + floor + ".");
                }
            }

            printEnding(hero, difficulty);
        }
        catch (GameInterruptedException interrupted)
        {
            System.out.println(abyss.ui.Language.t("\nInput closed."));
            if (hero != null)
            {
                printEnding(hero, difficulty);
            }
            else
            {
                System.out.println(abyss.ui.Language.t("The abyss waits for another day. Farewell."));
            }
        }
        // System.in belongs to the host application; do not close it here.
    }

    private void printEnding(Hero hero, Difficulty difficulty) {
        if (gameWon || !hero.isAlive()) {
            SaveData finalRecord = SaveData.capture(Math.min(FINAL_FLOOR, Math.max(1, STATS.getFloorReached())),
                    difficulty, hero, STATS, RANDOM, mechanismEncounterUsed, routes)
                    .withIdentity(expeditionSeed, gameWon && hero.isAlive() ? 1 : 2);
            if (!SaveManager.save(finalRecord)) System.out.println(abyss.ui.Language.t("Final archive could not be saved. The previous checkpoint remains."));
        }
        System.out.println(abyss.ui.Language.t("\n  EXPEDITION SEED  /  ") + expeditionSeed);
        achievements.check(hero, STATS, difficulty, gameWon && hero.isAlive());
        ConsolePresentation.printEnding(hero, gameWon, STATS, FINAL_FLOOR);
        abyss.ui.ConsoleAchievements.print(achievements, true);
        System.out.println(abyss.ui.Language.t("  Return to the gateway next launch to view your full chronicle.\n"));
    }
}

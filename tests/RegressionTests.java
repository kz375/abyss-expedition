import abyss.combat.*;
import abyss.config.Difficulty;
import abyss.content.*;
import abyss.core.*;
import abyss.puzzle.*;
import abyss.save.*;
import abyss.ui.ConsoleInput;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Dependency-free regression checks. Run with assertions enabled in a temporary save directory. */
public final class RegressionTests {
    private static int checks;
    private static final CombatResolver NO_DAMAGE = new CombatResolver() {
        public int hitEnemy(HeroContext h, Enemy e, double m) { return 0; }
        public int hitHero(HeroContext h, Enemy e, double m) { return 0; }
    };
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static Hero hero(HeroClass type) {
        return new Hero("Audit", type, Difficulty.ADVENTURER, new Random(17), NO_DAMAGE, new RunStatistics());
    }
    public static void main(String[] args) throws Exception {
        Path saveDir = Files.createTempDirectory("abyss-save-audit-");
        System.setProperty("abyss.saveDir", saveDir.toString());
        PrintStream output = System.out;
        try (PrintStream quiet = new PrintStream(OutputStream.nullOutputStream())) {
            System.setOut(quiet);
            shop(); combat(); ultraNightmareBoss(); damage(); relics(); saves(saveDir); puzzle(); achievements(); gameFlow(saveDir); seededSaves(saveDir); languages();
        } finally { System.setOut(output); }
        output.println("PASS: " + checks + " regression assertions. Isolated saves: " + saveDir);
    }
    private static void shop() {
        Hero offering = hero(HeroClass.WARRIOR);
        offering.addShield(100);
        check(offering.spendHealth(15) && offering.getHealth() == 205 && offering.getShield() == 100,
                "health offerings cannot be paid with shields");
        check(!offering.spendHealth(205) && offering.getHealth() == 205, "lethal offering rejected");
        Enemy ambush = Enemy.spawnEvent(3,true,Difficulty.ADVENTURER,new Random(1),n->{});
        check(ambush.isElite() && !ambush.isBoss(), "floor-three event spawns promised elite, not boss");
        Hero hero = hero(HeroClass.WARRIOR); hero.addGold(1000);
        Locations.visitShop(new LocationServices() {
            public int nextInt(int bound) { return 0; }
            public int readChoice(int min, int max) { return 1; }
        }, hero);
        check(hero.getGold() == 950, "shop charges exactly 18+32+35");
        check(hero.getPotions() == 3 && hero.getAttack() == 34 && hero.getDefense() == 14, "three purchases apply once");
        Hero nightmareShopper = new Hero("Nightmare", HeroClass.WARRIOR, Difficulty.NIGHTMARE, new Random(17), NO_DAMAGE, new RunStatistics());
        nightmareShopper.addGold(1000);
        Locations.visitShop(new LocationServices() { public int nextInt(int bound) { return 0; } public int readChoice(int min, int max) { return 1; } }, nightmareShopper);
        check(nightmareShopper.getAttack() == 36 && nightmareShopper.getDefense() == 16, "Nightmare shop upgrades grant +4 attack and defense");
        Hero ultraShopper = new Hero("Ultra", HeroClass.WARRIOR, Difficulty.ULTRA_NIGHTMARE, new Random(17), NO_DAMAGE, new RunStatistics());
        ultraShopper.addGold(1000);
        Locations.visitShop(new LocationServices() { public int nextInt(int bound) { return 0; } public int readChoice(int min, int max) { return 1; } }, ultraShopper);
        check(ultraShopper.getAttack() == 124 && ultraShopper.getDefense() == 50, "Ultra Nightmare shop upgrades grant +6 attack and defense");
        Hero leaving = hero(HeroClass.WARRIOR);
        final int[] visits = {0};
        Locations.visitShop(new LocationServices() {
            public int nextInt(int bound) { return 0; }
            public int readChoice(int min, int max) {
                if (visits[0]++ == 0) return 1;
                check(max == 3, "leave option shrinks after purchase"); return max;
            }
        }, leaving);
        FloorRoutes routes = new FloorRoutes(new int[]{1,2},0);
        ConsoleInput input = new ConsoleInput(new Scanner("3\n3\n1\n"));
        routes.choose(input); check(routes.visited() == 1, "side node reserved");
        check(routes.choose(input).getLabel().equals("Battle"), "visited side refused, combat stays available");
    }
    private static void combat() {
        Hero paladin = hero(HeroClass.PALADIN);
        RunStatistics stats = new RunStatistics();
        Enemy enemy = Enemy.spawn(1,false,Difficulty.ADVENTURER,8,new Random(1),n->{});
        BattleEngine engine = new BattleEngine(new ConsoleInput(new Scanner("4\n4\n5\n")),stats);
        check(engine.battle(paladin,enemy), "retreat returns escaped");
        check(stats.getTotalTurns() == 1, "view status doesn't retrigger turn");
        check(paladin.getShield() == 0, "retreat cannot farm passive shields");
        Hero warrior = hero(HeroClass.WARRIOR);
        warrior.applyStatus("Weak",1,5,false);
        warrior.beginTurn(); check(warrior.getCurrentAttack() == 27,"one-turn weak survives action");
        warrior.endTurn(); check(warrior.getCurrentAttack() == 32,"weak expires after action");
        warrior.beginTurn(); warrior.applyStatus("Regen",2,5,false); warrior.endTurn();
        check(warrior.findStatus("Regen").getTurns() == 2, "new self-status keeps its duration");
        warrior.beginTurn(); warrior.endTurn();
        check(warrior.findStatus("Regen").getTurns() == 1, "regen ticks once");
        warrior.beginTurn(); warrior.endTurn();
        check(warrior.findStatus("Regen") == null,"regen expires after two ticks");
        Hero mage = hero(HeroClass.MAGE);
        mage.startSkillCooldown(); check(mage.getSkillCooldown() == 1, "mage base cooldown");
        mage.decrementSkillCooldown(); check(mage.getSkillCooldown() == 0, "cooldown decrements");
    }
    private static void ultraNightmareBoss() {
        RunStatistics stats = new RunStatistics();
        int[] heroHits = {0}, enemyHits = {0};
        CombatResolver resolver = new CombatResolver() {
            public int hitEnemy(HeroContext hero, Enemy target, double multiplier) {
                int damage = enemyHits[0]++ == 0 ? 1 : 100000;
                target.takeDamage(damage); return damage;
            }
            public int hitHero(HeroContext hero, Enemy attacker, double multiplier) { heroHits[0]++; return 0; }
        };
        Hero hero = new Hero("Mechanic", HeroClass.WARRIOR, Difficulty.ULTRA_NIGHTMARE, new Random(21), resolver, stats);
        Enemy boss = Enemy.spawn(8, false, Difficulty.ULTRA_NIGHTMARE, 8, new Random(9), stats::addDamageDealt);
        new BattleEngine((minimum, maximum) -> stats.getTotalTurns() == 1 ? 1 : 2, stats).battle(hero, boss);
        check(heroHits[0] >= 2, "Ultra Nightmare boss basic attack summons a foe that joins the same enemy turn");
        check(enemyHits[0] == 2 && !boss.isAlive(), "skill attacks do not summon and can finish the boss");
        Enemy sampler = Enemy.spawn(8, false, Difficulty.ULTRA_NIGHTMARE, 8, new Random(44), n -> { });
        int elites = 0;
        for (int index = 0; index < 1000; index++) if (sampler.summonUltraNightmareMinion().isElite()) elites++;
        check(elites >= 25 && elites <= 75, "Ultra Nightmare boss summon elite rate remains near five percent");
    }
    private static void relics() {
        for (RelicEffect relic : RelicCatalog.all()) {
            Hero h = hero(HeroClass.WARRIOR);
            h.equipRelic(relic); h.equipRelic(relic);
            check(h.getRelicCount() == 1, "no duplicate relic " + relic.getName());
            h.loseRandomRelic(new Random(1));
            check(h.getAttack()==32 && h.getDefense()==12 && h.getMaxHealth()==220
                    && h.getCritBonus()==0 && h.getPotionBonusPct()==0 && h.getGoldBonusPct()==0
                    && h.getCooldownReduction()==0 && !h.hasPhoenix(), "unequip reverses " + relic.getName());
        }
        Hero h = hero(HeroClass.WARRIOR); h.takeDamage(100); h.applyMechanismFailure();
        check(h.getHealth() == h.getStartingHealth(), "agreed failure HP floor can restore low health");
        check(h.getGold() == 33, "failure gold rounded up");
    }
    private static void damage() {
        Hero h=hero(HeroClass.WARRIOR);
        RelicEffect fang=RelicCatalog.all().stream().filter(r->r.getName().equals("Vampire Fang")).findFirst().orElseThrow();
        h.equipRelic(fang); h.takeDamage(100);
        Enemy shielded=Enemy.spawn(1,false,Difficulty.ULTRA_NIGHTMARE,8,new Random(1),n->{});
        shielded.prepareUltraNightmare();
        RunStatistics stats=new RunStatistics();
        int dealt=CombatCalculator.hitEnemy(h,shielded,1,stats,new Random(2));
        check(dealt==0&&stats.getDamageDealt()==0&&h.getHealth()==120,"shield damage does not heal or inflate HP damage");
        Enemy weak=Enemy.spawn(1,false,Difficulty.EXPLORER,8,new Random(1),n->{});
        int hp=weak.getHealth(); h.addAttack(10000);
        check(CombatCalculator.hitEnemy(h,weak,1,stats,new Random(2))==hp,"overkill capped at remaining HP");
        Hero tank=hero(HeroClass.WARRIOR); tank.addShield(10000);
        tank.equipRelic(RelicCatalog.all().stream().filter(r->r.getName().equals("Thorn Mail")).findFirst().orElseThrow());
        Enemy attacker=Enemy.spawn(1,false,Difficulty.ADVENTURER,8,new Random(1),n->{});
        int before=attacker.getHealth();
        check(CombatCalculator.hitHero(tank,attacker,1,stats,new Random(2))==0&&attacker.getHealth()==before,"no thorns from fully blocked hit");
        Hero all=hero(HeroClass.WARRIOR);
        for(RelicEffect relic:RelicCatalog.all()) all.equipRelic(relic);
        all.spendGold(all.getGold()); all.takeDamage(100);
        MechanismTrial.settle(PuzzleEncounter.Result.VICTORY,all,new Random(1),new RunStatistics(),new ConsoleInput(new Scanner("")));
        check(all.getGold()==40&&all.getHealth()==all.getMaxHealth(),"all-relic compensation and zero-balance bonus");
    }
    private static void saves(Path directory) throws Exception {
        Hero h = hero(HeroClass.MAGE);
        for (RelicEffect relic : RelicCatalog.all()) h.equipRelic(relic);
        RunStatistics stats = new RunStatistics(); stats.addEliteKill(); stats.setFloorReached(5);
        Random rng = new Random(42); rng.nextInt();
        SaveData data = SaveData.capture(5,Difficulty.ADVENTURER,h,stats,rng,true,new FloorRoutes(new int[]{0,2},1));
        int expected = rng.nextInt();
        check(SaveManager.save(data), "save valid state");
        SaveData loaded = SaveManager.load().orElseThrow();
        check(loaded.getRandom().nextInt()==expected,"random state frozen at capture");
        check(loaded.getRandom().nextInt()==expected,"random getter returns independent state");
        check(loaded.isMechanismEncounterUsed(),"once-per-run flag persists");
        check(loaded.restoreRoutes(new Random()).visited()==1,"visited route persists");
        Hero restored=hero(HeroClass.MAGE);
        loaded.restoreHero(restored, RelicCatalog.all());
        check(restored.getAttack()==h.getAttack() && restored.getMaxHealth()==h.getMaxHealth()
                && restored.getRelicCount()==13 && restored.hasPhoenix(),"relics restored without double bonus");
        RunStatistics restoredStats = new RunStatistics(); loaded.restoreStatistics(restoredStats);
        check(restoredStats.getEliteKills()==1,"statistics persist");
        check(!SaveData.capture(9,Difficulty.ADVENTURER,h,stats,rng,false).isCompatible(),"reject invalid floor");
        Files.writeString(directory.resolve("abyss-expedition.save"),"broken");
        check(SaveManager.load().isEmpty(),"corrupt save handled");
        check(SaveManager.save(data) && SaveManager.delete() && !SaveManager.hasSave(),"delete works");
    }
    private static void puzzle() {
        PuzzleBoard fallback = new PuzzleBoard(new PuzzleBoard.State(9,19,18,34,3),21,45,new Random(1));
        List<PuzzleBoard.Direction> path = PuzzleLayouts.solve(fallback,500000);
        check(!path.isEmpty(),"fallback has a <=40 move solution");
        for (PuzzleBoard.Direction d:path) check(fallback.move(d),"solution moves valid");
        check(fallback.won(),"fallback wins");
        for (int seed=0;seed<16;seed++) {
            PuzzleBoard board=PuzzleLayouts.generate(new Random(seed));
            java.awt.Point m=board.monster();
            check(m.x>=1&&m.x<=5&&m.y>=1&&m.y<=5,"monster starts central");
            List<PuzzleBoard.Direction> solution=PuzzleLayouts.solve(board,500000);
            check(!solution.isEmpty()&&solution.size()<=40,"seed "+seed+" is solvable");
            for(PuzzleBoard.Direction d:solution) board.move(d);
            check(board.won(),"verified solution replays seed "+seed);
        }
        PuzzleBoard edge = new PuzzleBoard(new PuzzleBoard.State(12,14,13,34,3),21,45,new Random(3));
        check(edge.move(PuzzleBoard.Direction.RIGHT),"edge push accepted");
        java.awt.Point m=edge.monster();
        check(m.x>=1&&m.x<=5&&m.y>=1&&m.y<=5,"edge monster respawns central");
        check(!edge.boxes().contains(m)&&!edge.player().equals(m)&&!edge.targets().contains(m),"respawn avoids occupied tiles");
        PuzzleBoard limit=new PuzzleBoard(new PuzzleBoard.State(9,19,18,34,3),21,45,new Random(1));
        for(int i=0;i<40;i++) limit.move(i%2==0?PuzzleBoard.Direction.UP:PuzzleBoard.Direction.DOWN);
        check(limit.finished()&&!limit.won()&&limit.moves()==40,"40 moves causes defeat");
        check(!limit.move(PuzzleBoard.Direction.RIGHT),"terminal board rejects moves");
        check(PuzzleEncounter.playResult(new Random(1))==PuzzleEncounter.Result.UNAVAILABLE,"headless launch returns instead of hanging");
    }
    private static String launch(Path directory,String input) throws Exception {
        Process p=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-Djava.awt.headless=true","-Dfile.encoding=UTF-8","-Dabyss.saveDir="+directory,
                "-cp",System.getProperty("java.class.path"),"gameBody").redirectErrorStream(true).start();
        try(var out=p.getOutputStream()) { out.write(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
        String output=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        check(p.waitFor()==0,"game process exits cleanly");
        return output;
    }
    private static void gameFlow(Path directory) throws Exception {
        String menu = launch(directory,"3\n\n5\n");
        check(menu.contains("[2]  Load save / enter seed"),"seed restore entry always visible without current checkpoint");
        check(menu.contains("CHRONICLE OF THE ABYSS") && !SaveManager.hasSave(), "view achievements and exit without starting a game");
        String start=launch(directory,"1\n12345\nFlowTest\n1\n1\n");
        check(start.contains("Checkpoint saved")&&SaveManager.hasSave(),"new game checkpoint exists before first battle");
        SaveData first=SaveManager.load().orElseThrow();
        byte[] beforeMenu = Files.readAllBytes(directory.resolve("abyss-expedition.save"));
        launch(directory,"3\n\n5\n");
        check(Arrays.equals(beforeMenu,Files.readAllBytes(directory.resolve("abyss-expedition.save"))), "home viewing and quitting preserves checkpoint bytes");
        String resumed=launch(directory,"1\n");
        check(resumed.contains("Checkpoint restored"),"real entry point continues");
        SaveData second=SaveManager.load().orElseThrow();
        check(Arrays.equals(first.restoreRoutes(new Random()).types(),second.restoreRoutes(new Random()).types()),"EOF resume preserves menu");
        check(first.getRandom().nextInt()==second.getRandom().nextInt(),"EOF resume does not reroll RNG");
        launch(directory,"2\n");
        check(SaveManager.hasSave() && SaveManager.loadSeed(12345).isPresent(),"cancelled seed selection retains previous save");
        Hero dying=hero(HeroClass.WARRIOR); dying.takeDamage(dying.getHealth()-1);
        RunStatistics stats=new RunStatistics(); stats.setFloorReached(8);
        SaveManager.save(SaveData.capture(8,Difficulty.ADVENTURER,dying,stats,new Random(1),false));
        String death=launch(directory,"1\n" + "1\n".repeat(20));
        check(death.contains("You fell in the abyss") && death.contains("Thank you for venturing into the abyss.") && !SaveManager.hasSave(),"death clears checkpoint: " + death);
        Hero winning=hero(HeroClass.WARRIOR); winning.addAttack(10000);
        SaveManager.save(SaveData.capture(8,Difficulty.ADVENTURER,winning,stats,new Random(1),false));
        String victory=launch(directory,"1\n1\n1\n");
        check(victory.contains("You claim the Star Relic") && victory.contains("Thank you for venturing into the abyss.") && !SaveManager.hasSave(),"victory clears checkpoint");
        var profile = new abyss.achievement.AchievementBook(directory);
        check(profile.unlocked().contains(abyss.achievement.Achievement.STAR_BEARER), "real victory persists achievement after run save deletion");
        check(victory.contains("CHRONICLE OF THE ABYSS") && victory.contains("Star Bearer  NEW"), "ending displays newly earned achievement");
        SaveManager.save(SaveData.capture(8,Difficulty.ADVENTURER,winning,stats,new Random(1),false));
        launch(directory,"1\n1\n");
        check(!SaveManager.hasSave(),"EOF at final reward cannot revive completed expedition");
    }

    private static void achievements() throws Exception {
        Path dir = Files.createTempDirectory("abyss-achievement-audit-");
        var book = new abyss.achievement.AchievementBook(dir);
        Hero hero = hero(HeroClass.WARRIOR);
        RunStatistics stats = new RunStatistics();
        book.check(hero,stats,Difficulty.ADVENTURER,false);
        check(book.unlocked().isEmpty(), "empty run unlocks nothing");
        stats.addNormalKill();
        book.check(hero,stats,Difficulty.ADVENTURER,false);
        check(book.unlocked().size()==1, "first kill unlocks only first blood");
        book.check(hero,stats,Difficulty.ADVENTURER,false);
        check(book.recent().size()==1, "repeated check does not duplicate unlock");
        var reloaded = new abyss.achievement.AchievementBook(dir);
        check(reloaded.unlocked().size()==1 && reloaded.recent().isEmpty(), "reload preserves permanent progress, clears new badges");
        reloaded.check(hero,stats,Difficulty.NIGHTMARE,true);
        check(reloaded.unlocked().contains(abyss.achievement.Achievement.NIGHTMARE_CONQUEROR)
                && !reloaded.unlocked().contains(abyss.achievement.Achievement.ABYSS_MASTER), "nightmare win cannot unlock ultra feat");
        stats.addEliteKill(); stats.addBossKill(); stats.setFloorReached(5); stats.addGoldEarned(300); stats.addDamageDealt(1000);
        for(var relic : RelicCatalog.all().subList(0,5)) hero.equipRelic(relic);
        reloaded.check(hero,stats,Difficulty.ULTRA_NIGHTMARE,true);
        check(reloaded.unlocked().size()==10, "all statistic-based feats unlock at thresholds");
        reloaded.unlock(abyss.achievement.Achievement.TWIN_CORES); reloaded.save();
        check(new abyss.achievement.AchievementBook(dir).unlocked().size()==11, "trial achievement persists separately");
        reloaded.recordMonster("Elite Cave Bat"); reloaded.recordMonster("Cave Bat"); reloaded.recordMonster("Abyss Lord"); reloaded.save();
        var codex = new abyss.achievement.AchievementBook(dir);
        check(codex.monsterKillCount("Cave Bat") == 2 && codex.monsterKillCount("Abyss Lord") == 1,
                "monster codex records base and boss kill counts permanently");
        check(abyss.content.MonsterType.values().length == 21 && abyss.content.MonsterType.fromName("Elite Cave Bat") != null,
                "expanded monster roster has stable codex identifiers");
        try { reloaded.unlocked().clear(); check(false,"immutable achievement snapshot"); }
        catch(UnsupportedOperationException expected) { check(true,"immutable achievement snapshot"); }
        Path broken = Files.createTempDirectory("abyss-achievement-corrupt-");
        Path file = broken.resolve("achievements.properties");
        Files.writeString(file,"bad=" + "\\" + "uZZZZ");
        byte[] original=Files.readAllBytes(file);
        var corrupt=new abyss.achievement.AchievementBook(broken);
        corrupt.unlock(abyss.achievement.Achievement.FIRST_BLOOD); corrupt.save();
        check(Arrays.equals(original,Files.readAllBytes(file)), "unreadable achievement profile is never overwritten");
        var input = new abyss.ui.ConsoleInput(new Scanner("kz\n6\n"));
        check(abyss.ui.ConsoleMenus.chooseClass(input,Difficulty.ADVENTURER)==HeroClass.CREATOR, "restyled class screen preserves hidden selection");
    }

    private static byte[] bytes(SaveData data) throws Exception {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try(ObjectOutputStream objects=new ObjectOutputStream(output)) { objects.writeObject(data); }
        return output.toByteArray();
    }
    private static void seededSaves(Path directory) throws Exception {
        Hero original=hero(HeroClass.MAGE);
        for(var relic : RelicCatalog.all()) original.equipRelic(relic);
        original.gainExperience(200); original.takeDamage(31); original.addShield(17);
        original.addGold(87); original.startSkillCooldown();
        RunStatistics stats=new RunStatistics(); stats.setFloorReached(5); stats.addEliteKill(); stats.addDamageDealt(234);
        Random random=new Random(444); for(int i=0;i<31;i++) random.nextInt();
        FloorRoutes routes=new FloorRoutes(new int[]{1,0},1);
        SaveData snapshot=SaveData.capture(5,Difficulty.ADVENTURER,original,stats,random,true,routes).withIdentity(-888,0);
        check(SaveManager.save(snapshot),"seeded checkpoint saves");
        SaveData loaded=SaveManager.loadSeed(-888).orElseThrow();
        Hero restored=hero(HeroClass.MAGE);
        loaded.restoreHero(restored,RelicCatalog.all());
        RunStatistics restoredStats=new RunStatistics(); loaded.restoreStatistics(restoredStats);
        SaveData roundTrip=SaveData.capture(loaded.getFloor(),loaded.getDifficulty(),restored,restoredStats,
                loaded.getRandom(),loaded.isMechanismEncounterUsed(),loaded.restoreRoutes(new Random())).withIdentity(-888,0);
        check(Arrays.equals(bytes(snapshot),bytes(roundTrip)),"all checkpoint fields roundtrip exactly through disk and hero restoration");
        Random a=snapshot.getRandom(), b=loaded.getRandom();
        for(int i=0;i<100;i++) check(a.nextLong()==b.nextLong(),"continued RNG sequence matches " + i);
        byte[] archive=Files.readAllBytes(directory.resolve("expeditions/-888.save"));
        String fresh=launch(directory,"2\n777\nSecondHero\n1\n1\n");
        check(fresh.contains("Seed 777") && SaveManager.loadSeed(777).isPresent(),"new seed creates independent expedition");
        check(Arrays.equals(archive,Files.readAllBytes(directory.resolve("expeditions/-888.save"))),"new expedition retains previous archive byte for byte");
        String resumed=launch(directory,"2\n-888\n");
        check(resumed.contains("Checkpoint restored. Returning to Abyss Floor 5") && !resumed.contains("Enter your adventurer name"),"entering old seed restores instead of recreating");
        check(Arrays.equals(bytes(snapshot),bytes(SaveManager.loadSeed(-888).orElseThrow())),"old seed resume at EOF preserves complete checkpoint");
        String invalid=launch(directory,"2\nnot-a-number\n9223372036854775808\nLIST\n777\n");
        check(invalid.contains("Enter a whole number") && invalid.contains("IN PROGRESS") && invalid.contains("Checkpoint restored"),"seed input validates overflow and lists archives");
        Path broken=directory.resolve("expeditions/999.save"); Files.writeString(broken,"not a save");
        launch(directory,"2\n999\n");
        check(Files.readString(broken).equals("not a save"),"corrupt seed never starts new expedition or overwrites data");
        SaveData victory=SaveData.capture(8,Difficulty.ADVENTURER,original,stats,random,true,routes).withIdentity(4444,1);
        check(SaveManager.save(victory),"terminal archive saved");
        String ended=launch(directory,"1\n4444\n");
        check(ended.contains("Read-only final record") && ended.contains("VICTORY") && !ended.contains("ENCOUNTER:"),"completed seed restores final state without replaying combat");
        check(!SaveManager.save(SaveData.capture(1,Difficulty.ADVENTURER,hero(HeroClass.WARRIOR),new RunStatistics(),new Random(),false).withIdentity(4444,0)),"completed archive cannot be overwritten by fresh state");
        check(SaveManager.loadSeed(4444).orElseThrow().getOutcome()==1,"final record retained");
        original.takeDamage(original.getHealth()+original.getShield()+99999);
        if(original.isAlive()) original.takeDamage(original.getHealth()+original.getShield()+99999);
        SaveData defeat=SaveData.capture(5,Difficulty.ADVENTURER,original,stats,random,true,routes).withIdentity(Long.MIN_VALUE,2);
        check(SaveManager.save(defeat),"zero-health defeat retained");
        check(SaveManager.loadSeed(Long.MIN_VALUE).orElseThrow().getOutcome()==2,"minimum long seed and death reload");
        byte[] profile=Files.readAllBytes(directory.resolve("achievements.properties"));
        String generated=launch(directory,"1\n\nGenerated\n1\n1\n");
        check(generated.contains("New expedition seed:") && SaveManager.load().orElseThrow().hasSeed(),"blank input generates and persists seed");
        check(Arrays.equals(profile,Files.readAllBytes(directory.resolve("achievements.properties"))),"switching seeds retains achievement profile");
        String leaving=launch(directory,"1\n5\n");
        check(leaving.contains("EXPEDITION PAUSED") && SaveManager.load().isPresent(), "route menu leaves expedition with checkpoint retained");
        byte[] checkpoint=bytes(SaveManager.load().orElseThrow());
        try(var lock=SaveSessionLock.acquire()) {
            check(lock != null,"session lock acquired");
            check(launch(directory,"1\n").contains("Another game is using"),"second game cannot overwrite same directory");
        }
        check(Arrays.equals(checkpoint,bytes(SaveManager.load().orElseThrow())),"blocked second process preserves progress");
        Path deterministicA=Files.createTempDirectory("abyss-seed-a-");
        Path deterministicB=Files.createTempDirectory("abyss-seed-b-");
        launch(deterministicA,"1\n0\nSameHero\n2\n2\n5\n");
        launch(deterministicB,"1\n0\nSameHero\n2\n2\n5\n");
        check(Arrays.equals(Files.readAllBytes(deterministicA.resolve("expeditions/0.save")),
                Files.readAllBytes(deterministicB.resolve("expeditions/0.save"))),"zero seed creates identical starting state in independent directories");
    }

    private static void languages() throws Exception {
        Path dir=Files.createTempDirectory("abyss-language-audit-");
        String chinese=launch(dir,"6\n2\n3\n\n1\n333\nWarrior\n1\n1\n5\n");
        check(chinese.contains("深渊之门") && chinese.contains("成就图鉴"),"home switches immediately to Chinese");
        check(chinese.contains("深渊成就录") && chinese.contains("初战告捷"),"achievement titles and descriptions translated");
        check(chinese.contains("选择你的角色") && chinese.contains("每场战斗开始时获得 60 护盾"),"class descriptions translated");
        check(chinese.contains("远征已暂停") && chinese.contains("远征战报"),"ending translated");
        check(chinese.contains("Warrior  /  战士"),"user name remains untouched while class display is translated");
        Path archive=dir.resolve("expeditions/333.save");
        byte[] snapshot=Files.readAllBytes(archive);
        String next=launch(dir,"5\n");
        check(next.contains("深渊之门") && next.contains("继续冒险"),"language preference persists on next launch");
        String english=launch(dir,"6\n1\n5\n");
        check(english.contains("THE GATEWAY") && english.contains("Continue expedition"),"switch back to English works");
        check(Arrays.equals(snapshot,Files.readAllBytes(archive)),"language switching never changes seed archive bytes");
        String resume=launch(dir,"2\nLIST\n333\n5\n");
        check(resume.contains("Checkpoint restored") && resume.contains("Warrior  /  Warrior"),"Chinese-created save restores normally in English");
        check(Arrays.equals(snapshot,Files.readAllBytes(archive)),"cross-language restore preserves full checkpoint");
        String hidden=launch(dir,"6\n2\n2\n334\nHidden\n1\nkz\n6\n5\n");
        check(hidden.contains("造物主已显现") && hidden.contains("Hidden  /  造物主"),"KZ hidden input works in Chinese");
        check(launch(dir,"3\n\n5\n").contains("成就进度"),"Chinese achievement menu remains navigable with existing save");
        Files.writeString(dir.resolve("language.properties"),"language=" + "\\" + "uZZZZ");
        check(launch(dir,"5\n").contains("THE GATEWAY"),"damaged language settings fall back without blocking game");
        Files.delete(dir.resolve("abyss-expedition.save"));
        String archiveOnly=launch(dir,"6\n2\n2\nLIST\n333\n5\n");
        check(archiveOnly.contains("[2]  读取存档／输入种子"),"restore entry visible in Chinese without latest shortcut");
        check(archiveOnly.contains("存档已恢复") && !archiveOnly.contains("请输入冒险者名字"),"home option two restores retained archive even without active save");
        check(Arrays.equals(snapshot,Files.readAllBytes(archive)),"archive-only restore preserves full progress");
    }
}

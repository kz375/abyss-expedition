import abyss.combat.*;
import abyss.config.Difficulty;
import abyss.content.*;
import abyss.core.*;
import java.io.*;
import java.util.*;

/** Reproductions for the 1.2.6 review; no real saves or player data. */
public final class ReviewRegressionTests {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++; if (!ok) throw new AssertionError(label);
    }
    private static RelicEffect thorns() {
        return RelicCatalog.all().stream().filter(r -> r.getName().equals("Thorn Mail")).findFirst().orElseThrow();
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("abyss.saveDir", java.nio.file.Files.createTempDirectory("abyss-review-saves-").toString());
        PrintStream out = System.out;
        try (PrintStream quiet = new PrintStream(OutputStream.nullOutputStream())) {
            System.setOut(quiet);
            for (Difficulty d : Difficulty.values()) for (int f=1; f<=2; f++) for (int seed=0; seed<100; seed++) {
                Enemy normal = Enemy.spawn(f,false,d,8,new Random(seed), n -> {});
                Enemy elite = Enemy.spawn(f,true,d,8,new Random(seed), n -> {});
                int starter = Arrays.stream(HeroClass.values()).mapToInt(h -> (int)(h.getBaseHealth()*d.heroStatMultiplier())).max().orElseThrow();
                check(normal.getMaxHealth()>starter && normal.getMaxHealth()<=starter+80,"bounded opening health");
                check(elite.getMaxHealth()>normal.getMaxHealth(),"opening elite is tougher than ordinary foe");
            }
            RunStatistics stats = new RunStatistics();
            Hero plain = HeroFactory.create("Plain",HeroClass.WARRIOR,Difficulty.ADVENTURER,new Random(1),stats);
            Hero cursed = HeroFactory.create("Cursed",HeroClass.WARRIOR,Difficulty.ADVENTURER,new Random(1),stats);
            Enemy attacker = Enemy.spawn(5,false,Difficulty.ADVENTURER,8,new Random(2), n -> {});
            cursed.applyStatus("Curse",2,25,false);
            int base = CombatCalculator.hitHero(plain,attacker,1,stats,new Random(3));
            check(CombatCalculator.hitHero(cursed,attacker,1,stats,new Random(3))==base*125/100,"enemy curse actually increases damage");
            Enemy boss = Enemy.spawn(8,false,Difficulty.ULTRA_NIGHTMARE,8,new Random(4),n -> {});
            boss.takeDamage(boss.getHealth()-1); boss.setProtectedBySummons(true);
            Hero creator = HeroFactory.create("Creator",HeroClass.CREATOR,Difficulty.ULTRA_NIGHTMARE,new Random(1),stats);
            check(!HeroClass.CREATOR.tryExecution(creator,boss) && boss.isAlive(),"execution respects ward protection");
            int dealt = stats.getDamageDealt();
            thorns().onDamageTaken(creator,boss,100);
            check(stats.getDamageDealt()==dealt && boss.getHealth()==1,"blocked reflection records zero health damage");
            boss.setProtectedBySummons(false);
            check(HeroClass.CREATOR.tryExecution(creator,boss) && !boss.isAlive(),"execution works after protection ends");
            Hero thornHero = HeroFactory.create("Thorns",HeroClass.WARRIOR,Difficulty.ADVENTURER,new Random(1),stats);
            thornHero.equipRelic(thorns());
            Enemy leech = Enemy.spawn(5,false,Difficulty.ADVENTURER,8,new Random(5),n -> {});
            leech.takeDamage(leech.getHealth()-1);
            EnemyBehaviors.BLOOD_DRAIN.useSpecial(thornHero,leech);
            check(!leech.isAlive(),"drain cannot resurrect an attacker killed by reflection");
            summonProtection();
            trialReachability();
            interruptedDiscovery();
        } finally { System.setOut(out); }
        out.println("PASS: " + checks + " review regression assertions.");
    }
    private static void interruptedDiscovery() {
        var stats = new RunStatistics(); stats.setFloorReached(1);
        Hero hero = HeroFactory.create("Checkpoint",HeroClass.WARRIOR,Difficulty.ADVENTURER,new Random(1),stats);
        var routes = new FloorRoutes(new int[]{100,101,102,103},0);
        var data = abyss.save.SaveData.capture(1,Difficulty.ADVENTURER,hero,stats,new Random(1),false,routes).withIdentity(126,0);
        check(abyss.save.SaveManager.save(data),"discovery test checkpoint saved");
        InputStream previous = System.in;
        try {
            System.setIn(new ByteArrayInputStream("1\n3\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            new GameSession().run();
            check(abyss.save.SaveManager.loadSeed(126).orElseThrow().restoreRoutes(new Random()).visited()==0,
                    "interrupting event options does not consume the discovery without a reward");
            System.setIn(new ByteArrayInputStream("1\n3\n2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            new GameSession().run();
            var restored = abyss.save.SaveManager.loadSeed(126).orElseThrow().restoreRoutes(new Random());
            check(restored.visited()==15 && restored.chosenRoute()==0,"completed discovery persists the choice and locks alternatives");
        } finally { System.setIn(previous); }
    }
    private static void trialReachability() throws Exception {
        long seed = 0;
        while (new Random(seed).nextInt(100)>=5) seed++;
        InputStream previous = System.in;
        try {
            System.setIn(new ByteArrayInputStream("2\n2\n\n2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            int[] entered = {0};
            GameSession session = new GameSession(random -> { entered[0]++; return abyss.puzzle.PuzzleEncounter.Result.UNAVAILABLE; });
            var rng = GameSession.class.getDeclaredField("RANDOM"); rng.setAccessible(true); rng.set(session,new Random(seed));
            var event = GameSession.class.getDeclaredMethod("resolveEventCard",GameEvent.class,Hero.class,int.class,Difficulty.class);
            event.setAccessible(true);
            Hero hero = HeroFactory.create("Trial",HeroClass.WARRIOR,Difficulty.ADVENTURER,new Random(1),new RunStatistics());
            event.invoke(session,GameEvent.STARVED_IDOL,hero,4,Difficulty.ADVENTURER);
            check(entered[0]==0,"rare trial cannot start before floor five");
            event.invoke(session,GameEvent.STARVED_IDOL,hero,5,Difficulty.ADVENTURER);
            check(entered[0]==1,"named random event can reach the trial");
            event.invoke(session,GameEvent.STARVED_IDOL,hero,5,Difficulty.ADVENTURER);
            check(entered[0]==1,"trial remains once per expedition");
        } finally { System.setIn(previous); }
    }
    private static void summonProtection() {
        RunStatistics stats = new RunStatistics();
        Enemy boss = Enemy.spawn(8,false,Difficulty.ULTRA_NIGHTMARE,8,new Random(9),stats::addDamageDealt);
        int[] strikes = {0}; List<String> recorded = new ArrayList<>();
        CombatResolver resolver = new CombatResolver() {
            public int hitEnemy(HeroContext h,Enemy e,double m) { e.takeDamage(strikes[0]++==0?1:100000); return 0; }
            public int hitHero(HeroContext h,Enemy e,double m) {
                if (strikes[0]==1 && e==boss) check(boss.isProtectedBySummons(),"ward applies on the summoning turn");
                return 0;
            }
        };
        Hero hero = new Hero("Ward",HeroClass.WARRIOR,Difficulty.ULTRA_NIGHTMARE,new Random(10),resolver,stats);
        new BattleEngine((min,max) -> strikes[0]==0?1:strikes[0]==1?5:2,stats,recorded::add).battle(hero,boss);
        check(recorded.size()==1 && stats.getNormalKills()+stats.getEliteKills()==1,"summon death counted exactly once without loot");
    }
}

import abyss.combat.*;
import abyss.config.Difficulty;
import abyss.content.*;
import abyss.core.RunStatistics;
import abyss.puzzle.*;
import abyss.ui.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Run both locales through the same events and compare rules/RNG as well as displayed text. */
public class WorldLanguageTests {
    private static final class Services implements EventServices, LocationServices {
        final Random random = new Random(923); final int choice, roll;
        Services(int choice, int roll) { this.choice = choice; this.roll = roll; }
        public int readChoice(int min, int max) { return Math.max(min, Math.min(choice, max)); }
        public int nextInt(int bound) { return roll % bound; }
        public boolean nextBoolean() { return roll < 50; }
        public int gainGold(Hero hero, int amount) { hero.addGold(amount); return amount; }
        public List<RelicEffect> unownedRelics(Hero hero) { return RelicCatalog.all().stream().filter(r -> !hero.ownsRelic(r.getName())).toList(); }
        public RelicEffect randomElement(List<RelicEffect> relics) { return relics.get(random.nextInt(relics.size())); }
        public boolean resolveBattle(Hero hero, int floor, boolean elite, Difficulty difficulty) { return true; }
        public void resolveEvent(Hero hero, int floor, Difficulty difficulty) { }
        public void visitCamp(Hero hero) { Locations.visitCamp(this, hero); }
        public void visitShop(Hero hero) { Locations.visitShop(this, hero); }
        public void resolveMechanismEncounter(Hero hero, int floor, Difficulty difficulty) { }
        public boolean canEncounterMechanism() { return true; }
    }
    private static Hero hero() {
        Hero hero = HeroFactory.create("007", HeroClass.WARRIOR, Difficulty.ADVENTURER, new Random(23), new RunStatistics());
        hero.addGold(2000); return hero;
    }
    private static String snapshot(Hero h, Services s) {
        return h.getHealth()+":"+h.getMaxHealth()+":"+h.getAttack()+":"+h.getDefense()+":"+h.getShield()+":"+h.getGold()+":"+h.getPotions()+":"+h.getEquippedRelics().stream().map(RelicEffect::getName).toList()+":"+s.random.nextLong();
    }
    private static List<String> exercise() {
        List<String> states = new ArrayList<>();
        for (int roll : new int[]{0, 49, 60, 80, 99}) for (int choice = 1; choice <= 4; choice++) {
            for (GameEvent event : GameEvent.values()) {
                Services service = new Services(choice, roll); Hero hero = hero();
                event.resolve(service, hero, 5, Difficulty.ADVENTURER); states.add(snapshot(hero, service));
            }
            Services service = new Services(choice, roll); Hero camper = hero();
            Locations.visitCamp(service, camper); states.add(snapshot(camper, service));
            Hero shopper = hero(); Locations.visitShop(service, shopper); states.add(snapshot(shopper, service));
        }
        MechanismTrial.introduction(new ConsoleInput(new Scanner("\n")));
        for (var result : PuzzleEncounter.Result.values()) {
            Services service = new Services(1, 0); Hero hero = hero();
            hero.equipRelic(RelicCatalog.all().get(0));
            MechanismTrial.settle(result, hero, service.random, new RunStatistics(), new ConsoleInput(new Scanner("1\n")));
            states.add(snapshot(hero, service));
        }
        Hero complete = hero(); for (var relic : RelicCatalog.all()) complete.equipRelic(relic);
        MechanismTrial.settle(PuzzleEncounter.Result.VICTORY, complete, new Random(1), new RunStatistics(), new ConsoleInput(new Scanner("")));
        return states;
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("abyss.saveDir", Files.createTempDirectory("abyss-world-language-").toString());
        PrintStream original = System.out;
        var english = new ByteArrayOutputStream(); var chinese = new ByteArrayOutputStream();
        try {
            Language.select(false); System.setOut(new PrintStream(english, true, StandardCharsets.UTF_8));
            var en = exercise();
            Language.select(true); System.setOut(new PrintStream(chinese, true, StandardCharsets.UTF_8));
            var zh = exercise();
            if (!en.equals(zh)) throw new AssertionError("Locale changed event mechanics or RNG");
            var leftover = java.util.regex.Pattern.compile(".*[A-Za-z]{3,}.*").matcher(chinese.toString(StandardCharsets.UTF_8));
            if (leftover.find()) throw new AssertionError("Untranslated world text: " + leftover.group());
            if (english.toString(StandardCharsets.UTF_8).matches("(?s).*[\\p{IsHan}].*")) throw new AssertionError("Chinese leaked into English");
            var selection = ConsoleSeeds.choose(new ConsoleInput(new Scanner("列表\n123\n")));
            if (selection.seed() != 123) throw new AssertionError("Chinese archive command failed");
            check("an early swarm (Cave Bat, Abyss Hound, Lost Miner)".equals(Prophecy.visionForFloor(2)), "floor-two prophecy");
            check("Relic Guardian".equals(Prophecy.visionForFloor(3)), "boss prophecy");
            check("a shadow host (Shadow Assassin, Cursed Doll, Bone Scholar)".equals(Prophecy.visionForFloor(4)), "mid-floor prophecy");
            check("a lost legion (Void Stalker, Bone Reaper, Dread Wraith)".equals(Prophecy.visionForFloor(7)), "late-floor prophecy");
            check("Abyss Lord".equals(Prophecy.visionForFloor(8)), "final boss prophecy");
            check("nothing beyond the final gate".equals(Prophecy.visionForFloor(9)), "end-of-run prophecy");
            check(GameEvent.valueOf("RELIC_CURATOR") != null && GameEvent.valueOf("RIFT_GATE") != null
                            && GameEvent.valueOf("FORGOTTEN_FORGE") != null && GameEvent.valueOf("ECHOING_ALTAR") != null
                            && GameEvent.valueOf("MOONLIT_CARAVAN") != null && GameEvent.valueOf("STARVED_IDOL") != null,
                    "new event types registered");
            original.println("PASS: " + en.size() + " language-independent event/shop/trial snapshots; monolingual output and Chinese archive input.");
        } finally { System.setOut(original); Language.select(false); }
    }
    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); }
}

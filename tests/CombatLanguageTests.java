import abyss.combat.*;
import abyss.content.*;
import abyss.core.*;
import abyss.config.*;
import abyss.ui.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Chinese battle coverage plus exact language-independent combat and RNG comparisons. */
public class CombatLanguageTests {
    private static int checks;
    private static void check(boolean result,String label) { checks++; if(!result) throw new AssertionError(label); }
    private record Result(int health,int shield,int enemyHealth,int enemyShield,int potions,int cooldown,
                          int damageDealt,int damageTaken,int turns,long nextRandom) { }
    private static Result fight(HeroClass type,int seed,int floor) {
        Random random=new Random(seed); RunStatistics stats=new RunStatistics();
        Hero hero=HeroFactory.create("Warrior",type,Difficulty.ULTRA_NIGHTMARE,random,stats);
        for(var relic:RelicCatalog.all()) hero.equipRelic(relic);
        Enemy enemy=Enemy.spawn(floor,true,Difficulty.ULTRA_NIGHTMARE,8,random,stats::addDamageDealt);
        new BattleEngine((min,max)-> {
            if(stats.getTotalTurns()>1000) throw new AssertionError("Combat did not finish");
            if(hero.getHealth()<hero.getMaxHealth()/2&&hero.getPotions()>0) return 3;
            return hero.getSkillCooldown()==0?2:1;
        },stats).battle(hero,enemy);
        return new Result(hero.getHealth(),hero.getShield(),enemy.getHealth(),enemy.getShield(),hero.getPotions(),
                hero.getSkillCooldown(),stats.getDamageDealt(),stats.getDamageTaken(),stats.getTotalTurns(),random.nextLong());
    }
    public static void main(String[] args) throws Exception {
        System.setProperty("abyss.saveDir",Files.createTempDirectory("abyss-combat-language-").toString());
        PrintStream original=System.out;
        ByteArrayOutputStream chinese=new ByteArrayOutputStream();
        try(PrintStream quiet=new PrintStream(OutputStream.nullOutputStream());
            PrintStream capture=new PrintStream(chinese,true,StandardCharsets.UTF_8)) {
            for(HeroClass type:HeroClass.values()) for(int floor:new int[]{1,4,6,8}) {
                System.setOut(quiet); Language.select(false); Result en=fight(type,7,floor);
                Language.select(true); System.setOut(capture); Result zh=fight(type,7,floor);
                check(en.equals(zh),"identical mechanics and RNG: " + type + " floor " + floor);
            }
            String output=chinese.toString(StandardCharsets.UTF_8);
            check(!output.matches("(?s).*[A-Za-z]{3,}.*"),"no untranslated English in Chinese battle transcript: "
                    + firstEnglish(output));
            for(String phrase:List.of("精英词缀","行动预告","深渊护盾","护盾吸收了","凶猛撕咬","现实撕裂",
                    "使用药水","普通攻击","钢铁意志","血红宝石")) check(output.contains(phrase),"Chinese battle coverage: " + phrase);
            check(Language.t("Elite Lost Miner").equals("精英·迷失矿工"),"screenshot enemy name translated");
            check(Language.t("Elite Affix").equals("精英词缀"),"label is not mistaken for elite enemy prefix");
            check(Language.battle(" and ").equals(" 和 "),"battle conjunction is not input range separator");
            check(Language.t(" and ").equals(" 至 "),"input range grammar preserved");
            check(ConsoleBattle.statusDescription(StatusEffect.create("Burn",2,5)).equals("灼烧（剩余 2 回合，强度 5）"),"readable status duration");
            for(var relic:RelicCatalog.all()) {
                check(!Language.t(relic.getName()).equals(relic.getName()),"relic translated: " + relic.getName());
                check(!Language.t(relic.getDescription()).equals(relic.getDescription()),"relic description translated");
            }
        } finally { System.setOut(original); Language.select(false); }
        System.out.println("PASS: " + checks + " combat localization assertions.");
    }
    private static String firstEnglish(String text) {
        var matcher=java.util.regex.Pattern.compile(".*[A-Za-z]{3,}.*").matcher(text);
        return matcher.find()?matcher.group():"none";
    }
}

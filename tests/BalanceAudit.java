import abyss.combat.*;
import abyss.config.Difficulty;
import abyss.content.*;
import abyss.core.*;
import java.io.*;
import java.util.*;

/** Diagnostic, not a claim of human win rates. Fixed policy, 100 seeded expeditions per class/difficulty. */
public final class BalanceAudit {
    public static void main(String[] args) throws Exception {
        PrintStream report = System.out;
        try(PrintStream quiet = new PrintStream(OutputStream.nullOutputStream())) {
            for (Difficulty difficulty : Difficulty.values()) {
                for (HeroClass type : HeroClass.values()) {
                    int wins=0, floors=0, turns=0;
                    for(int seed=0;seed<100;seed++) {
                        System.setOut(quiet);
                        Random rng=new Random(seed);
                        RunStatistics stats=new RunStatistics();
                        CombatResolver resolver=new CombatResolver() {
                            public int hitEnemy(HeroContext h, Enemy e, double m) { return CombatCalculator.hitEnemy((Hero)h,e,m,stats,rng); }
                            public int hitHero(HeroContext h, Enemy e, double m) { return CombatCalculator.hitHero((Hero)h,e,m,stats,rng); }
                        };
                        Hero h=new Hero("Simulation",type,difficulty,rng,resolver,stats);
                        BattleEngine engine=new BattleEngine((min,max)-> {
                            if(stats.getTotalTurns()>2000) throw new AssertionError("Unbounded combat");
                            if(h.getPotions()>0&&h.getHealth()<h.getMaxHealth()*0.45) return 3;
                            return h.getSkillCooldown()==0 ? 2 : 1;
                        },stats);
                        for(int floor=1;floor<=8&&h.isAlive();floor++) {
                            floors++;
                            // One optional camp and one optional shop opportunity, not guaranteed in real routes.
                            if(floor>1) h.restoreHealth((int)(h.getMaxHealth()*0.35));
                            if(h.getGold()>=18&&h.getPotions()<3) { h.spendGold(18); h.receivePotion(); }
                            Enemy e=Enemy.spawn(floor,floor==2||floor==6,difficulty,8,rng,stats::addDamageDealt);
                            engine.battle(h,e);
                            if(!h.isAlive()) break;
                            if(e.isAlive()) throw new AssertionError("Battle ended with living foe");
                            h.addGold(e.getGoldReward()); h.gainExperience(e.getExperienceReward());
                            if(e.isBoss()||e.isElite()) {
                                List<RelicEffect> pool=RelicCatalog.all().stream().filter(r->!h.ownsRelic(r.getName())).toList();
                                if(!pool.isEmpty()) h.equipRelic(pool.get(rng.nextInt(pool.size())));
                            }
                            if(floor==8) wins++;
                        }
                        turns+=stats.getTotalTurns();
                    }
                    System.setOut(report);
                    report.printf("%-17s %-12s wins %3d/100 | average floor %.2f | rounds %.1f%n",
                            difficulty,type,wins,floors/100.0,turns/100.0);
                }
            }
        } finally { System.setOut(report); }
    }
}

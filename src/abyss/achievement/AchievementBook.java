package abyss.achievement;

import abyss.combat.Hero;
import abyss.config.Difficulty;
import abyss.core.RunStatistics;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Independent, monotonic profile: resuming a checkpoint cannot count a feat twice. */
public final class AchievementBook {
    private final Path file;
    private final EnumSet<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
    private final EnumSet<Achievement> recent = EnumSet.noneOf(Achievement.class);
    private final Properties properties = new Properties();
    private boolean writable = true;
    private boolean dirty;

    public AchievementBook(Path directory) {
        file = directory.resolve("achievements.properties");
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
            for (Achievement a : Achievement.values())
                if ("true".equals(properties.getProperty(a.name()))) unlocked.add(a);
        } catch (IOException | IllegalArgumentException e) {
            writable = false;
            System.out.println(abyss.ui.Language.t("Achievement profile could not be read. Existing file preserved; this session is read-only."));
        }
    }
    public Set<Achievement> unlocked() { return Set.copyOf(unlocked); }
    public Set<Achievement> recent() { return Set.copyOf(recent); }

    public void unlock(Achievement achievement) {
        if (unlocked.add(achievement)) {
            recent.add(achievement);
            properties.setProperty(achievement.name(), "true");
            dirty = true;
            System.out.println((abyss.ui.Language.isChinese() ? abyss.ui.Language.t("\n  ❉ 成就解锁  /  ") : abyss.ui.Language.t("\n  ❉ ACHIEVEMENT UNLOCKED  /  "))
                    + abyss.ui.Language.t(achievement.title()));
        }
    }
    public void check(Hero hero, RunStatistics s, Difficulty difficulty, boolean victory) {
        if (s.getNormalKills() + s.getEliteKills() + s.getBossKills() > 0) unlock(Achievement.FIRST_BLOOD);
        if (s.getEliteKills() > 0) unlock(Achievement.ELITE_HUNTER);
        if (s.getBossKills() > 0) unlock(Achievement.GUARDIAN_BREAKER);
        if (s.getFloorReached() >= 5) unlock(Achievement.DEEP_DELVER);
        if (hero.getRelicCount() >= 5) unlock(Achievement.RELIC_KEEPER);
        if (s.getGoldEarned() >= 300) unlock(Achievement.GOLD_SEEKER);
        if (s.getDamageDealt() >= 1000) unlock(Achievement.BATTLE_VETERAN);
        if (victory) {
            unlock(Achievement.STAR_BEARER);
            if (difficulty == Difficulty.NIGHTMARE || difficulty == Difficulty.ULTRA_NIGHTMARE)
                unlock(Achievement.NIGHTMARE_CONQUEROR);
            if (difficulty == Difficulty.ULTRA_NIGHTMARE) unlock(Achievement.ABYSS_MASTER);
        }
        save();
    }
    public void save() {
        if (!dirty) return;
        if (!writable) {
            System.out.println(abyss.ui.Language.t("  Achievement progress is session-only: profile is read-only."));
            return;
        }
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "achievements-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Abyss Expedition achievements / independent of expedition saves");
            }
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (IOException e) {
            System.out.println(abyss.ui.Language.t("  Achievement progress could not be saved; it remains available this session."));
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }
}

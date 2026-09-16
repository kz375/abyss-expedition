package abyss.save;

import java.io.IOException;
import java.nio.channels.*;
import java.nio.file.*;

/** Prevent two game windows from silently overwriting each other's seed progress. */
public final class SaveSessionLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private SaveSessionLock(FileChannel channel, FileLock lock) { this.channel = channel; this.lock = lock; }
    public static SaveSessionLock acquire() {
        FileChannel channel = null;
        try {
            Path directory = Path.of(System.getProperty("abyss.saveDir", "saves"));
            Files.createDirectories(directory);
            channel = FileChannel.open(directory.resolve("session.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            FileLock lock = channel.tryLock();
            if (lock != null) return new SaveSessionLock(channel, lock);
            channel.close();
            System.out.println(abyss.ui.Language.t("Another game is using this save directory. Close it before continuing."));
        } catch (IOException | OverlappingFileLockException e) {
            if (channel != null) try { channel.close(); } catch (IOException ignored) { }
            System.out.println(abyss.ui.Language.t("Cannot safely open the save directory: ") + e.getMessage());
        }
        return null;
    }
    @Override public void close() {
        try { lock.release(); } catch (IOException ignored) { }
        try { channel.close(); } catch (IOException ignored) { }
    }
}

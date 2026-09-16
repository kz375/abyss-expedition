package abyss.save;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** Seed archives are authoritative; the legacy filename is a last-active convenience copy. */
public final class SaveManager
{
    private static final Path SAVE_FILE = Path.of(System.getProperty("abyss.saveDir", "saves"), "abyss-expedition.save");

    private SaveManager() { }

    public static boolean hasSave() { return Files.isRegularFile(SAVE_FILE); }

    public static Optional<SaveData> load()
    {
        Optional<SaveData> latest = read(SAVE_FILE);
        if (latest.isPresent() && latest.get().hasSeed()) {
            latest = loadSeed(latest.get().getSeed());
        }
        return latest.filter(data -> data.getOutcome() == 0);
    }

    private static Path seedPath(long seed) { return SAVE_FILE.getParent().resolve("expeditions").resolve(Long.toString(seed) + ".save"); }
    public static boolean seedExists(long seed) { return Files.exists(seedPath(seed)); }
    public static long unusedSeed() {
        java.util.Random generator = new java.security.SecureRandom();
        long seed;
        do { seed = generator.nextLong(); } while (seedExists(seed));
        return seed;
    }
    public static Optional<SaveData> loadSeed(long seed) {
        return read(seedPath(seed)).filter(data -> data.hasSeed() && data.getSeed() == seed);
    }
    public static java.util.List<SaveData> archives() {
        Path directory = SAVE_FILE.getParent().resolve("expeditions");
        if (!Files.isDirectory(directory)) return java.util.List.of();
        java.util.List<SaveData> result = new java.util.ArrayList<>();
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(p -> p.getFileName().toString().matches("-?[0-9]+\\.save")).sorted().toList()) {
                try { loadSeed(Long.parseLong(path.getFileName().toString().replace(".save", ""))).ifPresent(result::add); }
                catch (NumberFormatException ignored) { }
            }
        } catch (IOException e) { System.out.println(abyss.ui.Language.t("Unable to list seed archives: ") + e.getMessage()); }
        return java.util.List.copyOf(result);
    }

    private static Optional<SaveData> read(Path file) {
        if (!Files.exists(file)) return Optional.empty();
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file)))
        {
            input.setObjectInputFilter(info -> {
                if (info.depth() > 16 || info.references() > 2000 || info.streamBytes() > 1_000_000 || info.arrayLength() > 2000)
                    return java.io.ObjectInputFilter.Status.REJECTED;
                Class<?> type = info.serialClass();
                if (type == null) return java.io.ObjectInputFilter.Status.UNDECIDED;
                return type == SaveData.class || type == abyss.config.Difficulty.class || type == Enum.class
                        || type == java.util.Random.class || type == java.util.ArrayList.class
                        || type == String.class || type == Object[].class || type == int[].class
                        ? java.io.ObjectInputFilter.Status.ALLOWED : java.io.ObjectInputFilter.Status.REJECTED;
            });
            Object value = input.readObject();
            if (value instanceof SaveData data && data.isCompatible()) return Optional.of(data);
        }
        catch (IOException | ClassNotFoundException | IllegalArgumentException ignored)
        {
            // A broken save must never prevent a new expedition from starting.
        }
        System.out.println(abyss.ui.Language.t("The checkpoint is damaged or incompatible. Preserved: ") + file);
        return Optional.empty();
    }

    public static boolean save(SaveData data)
    {
        if (!data.isCompatible()) return false;
        if (data.hasSeed()) {
            Path archive = seedPath(data.getSeed());
            if (Files.exists(archive)) {
                Optional<SaveData> prior = loadSeed(data.getSeed());
                if (prior.isEmpty() || prior.get().getOutcome() != 0) {
                    System.out.println(abyss.ui.Language.t("Existing archive is unreadable or completed; it will not be overwritten."));
                    return false;
                }
            }
            if (!write(archive, data)) return false;
        }
        if (data.getOutcome() != 0) { delete(); return true; }
        if (!write(SAVE_FILE, data)) {
            if (!data.hasSeed()) return false;
            System.out.println(abyss.ui.Language.t("Latest shortcut could not be updated. Restore using seed ") + data.getSeed() + ".");
        }
        return true;
    }

    private static boolean write(Path file, SaveData data) {
        Path temporary = null;
        try
        {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "checkpoint-", ".tmp");
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(temporary)))
            {
                output.writeObject(data);
            }
            try
            {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ignored)
            {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        }
        catch (IOException ignored)
        {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignoredToo) { }
            return false;
        }
    }

    public static boolean delete()
    {
        try { Files.deleteIfExists(SAVE_FILE); return true; }
        catch (IOException failure) { System.err.println("Unable to clear checkpoint: " + failure.getMessage()); return false; }
    }
}

package abyss.desktop;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

/** Desktop releases keep data outside read-only installation directories. */
public final class DesktopPaths {
    private DesktopPaths() { }
    public static Path defaultDirectory(String os, Path home, String appData, String xdgData) {
        String platform = os.toLowerCase(Locale.ROOT);
        if (platform.startsWith("windows"))
            return (appData == null || appData.isBlank() ? home.resolve("AppData/Roaming") : Path.of(appData)).resolve("AbyssExpedition");
        if (platform.contains("mac")) return home.resolve("Library/Application Support/AbyssExpedition");
        Path xdg = xdgData == null || xdgData.isBlank() ? null : Path.of(xdgData);
        return (xdg != null && xdg.isAbsolute() ? xdg : home.resolve(".local/share")).resolve("abyss-expedition");
    }
    public static void configure() throws IOException {
        if (System.getProperty("abyss.saveDir") != null) return;
        Path target = defaultDirectory(System.getProperty("os.name"), Path.of(System.getProperty("user.home")),
                System.getenv("APPDATA"), System.getenv("XDG_DATA_HOME"));
        importLegacy(Path.of("saves").toAbsolutePath(), target);
        System.setProperty("abyss.saveDir", target.toString());
    }
    /** First-launch copy only. Never merge, overwrite or delete a player's existing saves. */
    public static void importLegacy(Path legacy, Path target) throws IOException {
        if (Files.exists(target) || !Files.isDirectory(legacy)) return;
        if (!Files.exists(legacy.resolve("abyss-expedition.save")) && !Files.isDirectory(legacy.resolve("expeditions"))
                && !Files.exists(legacy.resolve("achievements.properties")) && !Files.exists(legacy.resolve("language.properties"))) return;
        Files.createDirectories(target.getParent());
        Path stage = Files.createTempDirectory(target.getParent(), "abyss-import-");
        try (var paths = Files.walk(legacy)) {
            for (Path source : paths.toList()) {
                if (Files.isSymbolicLink(source)) throw new IOException("Legacy save contains a symbolic link: " + source);
                Path relative = legacy.relativize(source);
                if (relative.toString().isEmpty()) continue;
                String name = source.getFileName().toString();
                if (name.equals("session.lock") || name.endsWith(".tmp")) continue;
                Path destination = stage.resolve(relative);
                if (Files.isDirectory(source)) Files.createDirectories(destination);
                else Files.copy(source, destination);
            }
        }
        // No replacement: a racing launch or existing destination must not be overwritten.
        Files.move(stage, target);
    }
}

package abyss.web;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/** Validate in a disposable JVM before making any uploaded archive active. */
final class ArchiveImport {
    static boolean allowed(String name) {
        return name.matches("abyss-expedition\\.save|expeditions/-?[0-9]+\\.save|achievements\\.properties|language\\.properties");
    }
    static void unpack(byte[] bytes, Path staging) throws IOException {
        Set<String> names = new HashSet<>(); int total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() && name.equals("expeditions/")) continue;
                if (!allowed(name) || !names.add(name) || names.size() > 256) throw new IOException("Unexpected archive entry");
                byte[] content = zip.readNBytes(1_000_001); total += content.length;
                if (content.length > 1_000_000 || total > 16_000_000) throw new IOException("Archive too large");
                Path target = staging.resolve(name); Files.createDirectories(target.getParent());
                Files.write(target, content, StandardOpenOption.CREATE_NEW);
            }
        }
        if (names.stream().noneMatch(n -> n.startsWith("expeditions/"))) throw new IOException("No seed archives");
        String executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process check = new ProcessBuilder(executable, "-Xmx64m", "-Dabyss.saveDir=" + staging,
                "-cp", System.getProperty("java.class.path"), "abyss.web.WebGame", "--validate")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!check.waitFor(10, TimeUnit.SECONDS)) { check.destroyForcibly(); throw new IOException("Validation timeout"); }
            if (check.exitValue() != 0) throw new IOException("Damaged or incompatible save");
        } catch (InterruptedException e) { check.destroyForcibly(); Thread.currentThread().interrupt(); throw new IOException(e); }
    }
    static void discardStaging(Path staging) throws IOException {
        // Only called on this import's freshly created staging directory, never a player directory.
        try (var files = Files.walk(staging)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }
}

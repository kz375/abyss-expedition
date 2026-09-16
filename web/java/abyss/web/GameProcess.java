package abyss.web;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/** Versioned prompts reject stale tabs and duplicate POSTs before they can spend a turn. */
final class GameProcess implements AutoCloseable {
    private final Process child;
    private final BufferedWriter input;
    private final StringBuilder pending = new StringBuilder();
    private String screen = "", puzzle = "null", language = "en";
    private final java.util.Map<String, String> histories = new java.util.HashMap<>();
    private long revision;
    private boolean ready, ended;
    private volatile long accessed = System.currentTimeMillis();

    GameProcess(Path directory) throws IOException {
        String executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        ProcessBuilder builder = new ProcessBuilder(executable, "-Xms16m", "-Xmx128m", "-Dfile.encoding=UTF-8",
                "-Djava.awt.headless=true", "-Dabyss.saveDir=" + directory.toAbsolutePath(),
                "-cp", System.getProperty("java.class.path"), "abyss.web.WebGame");
        // A bounded latest-run log; never append an unbounded log per player.
        builder.redirectError(directory.resolve("worker.log").toFile());
        child = builder.start();
        input = child.outputWriter(StandardCharsets.UTF_8);
        Thread reader = new Thread(() -> {
            try (BufferedReader output = child.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = output.readLine()) != null) accept(line);
            } catch (IOException | IllegalArgumentException e) {
                synchronized (this) { pending.append(language.equals("zh") ? "\n游戏连接已结束。\n" : "\nConnection to the game ended.\n"); }
            } finally {
                synchronized (this) { finish(); }
            }
        }, "web-game-output");
        reader.setDaemon(true); reader.start();
    }

    private synchronized void accept(String line) {
        if (line.startsWith("T\t")) {
            pending.append(new String(Base64.getDecoder().decode(line.substring(2)), StandardCharsets.UTF_8));
            if (pending.length() > 60000) pending.delete(0, pending.length() - 60000);
        } else if (line.startsWith("W\t")) {
            String[] fields = line.split("\t", 3);
            language = fields[1]; puzzle = fields[2];
            commitText(); ready = true; revision++;
        } else if (line.equals("E")) finish();
    }

    private void commitText() {
        if (!pending.isEmpty()) {
            screen = pending.toString(); pending.setLength(0);
            String history = histories.getOrDefault(language, "") + screen + "\n";
            if (history.length() > 100000) history = history.substring(history.length() - 100000);
            histories.put(language, history);
        }
    }

    private void finish() {
        if (ended) return;
        commitText(); ended = true; ready = false; puzzle = "null"; revision++;
    }

    synchronized boolean submit(long expected, String text) throws IOException {
        accessed = System.currentTimeMillis();
        if (!ready || ended || revision != expected) return false;
        ready = false; revision++;
        input.write(text); input.newLine(); input.flush();
        return true;
    }

    synchronized String snapshot() {
        accessed = System.currentTimeMillis();
        return "{\"revision\":" + revision + ",\"ready\":" + ready + ",\"ended\":" + ended
                + ",\"language\":" + Json.quote(language) + ",\"screen\":" + Json.quote(screen)
                + ",\"history\":" + Json.quote(histories.getOrDefault(language, "")) + ",\"puzzle\":" + puzzle + "}";
    }
    synchronized boolean ended() { return ended; }
    long lastAccess() { return accessed; }

    @Override public void close() {
        try { input.close(); } catch (IOException ignored) { }
        try {
            if (!child.waitFor(3, TimeUnit.SECONDS)) child.destroy();
            if (!child.waitFor(1, TimeUnit.SECONDS)) child.destroyForcibly();
        } catch (InterruptedException e) { child.destroyForcibly(); Thread.currentThread().interrupt(); }
    }
}

package abyss.web;

import abyss.core.GameSession;
import abyss.core.GameInterruptedException;
import abyss.puzzle.*;
import abyss.save.SaveSessionLock;
import abyss.ui.Language;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.file.*;

/** One child JVM per player: legacy streams, RNG, settings and save paths stay isolated. */
public final class WebGame {
    private final BufferedReader commands = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final PrintStream protocol = System.out;
    private final ByteArrayOutputStream text = new ByteArrayOutputStream();

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--validate")) {
            Path root = Path.of(System.getProperty("abyss.saveDir"));
            long expected = 0;
            if (Files.isDirectory(root.resolve("expeditions"))) try (var files = Files.list(root.resolve("expeditions"))) {
                expected = files.filter(p -> p.toString().endsWith(".save")).count();
            }
            if (expected == 0 || abyss.save.SaveManager.archives().size() != expected)
                throw new IOException("Backup has invalid or missing seed archives");
            if (Files.exists(root.resolve("abyss-expedition.save")) && abyss.save.SaveManager.load().isEmpty())
                throw new IOException("Invalid active checkpoint");
            return;
        }
        new WebGame().run();
    }

    private void run() throws Exception {
        System.setOut(new PrintStream(text, true, StandardCharsets.UTF_8));
        System.setErr(System.out);
        System.setIn(new InputStream() {
            private byte[] line = new byte[0];
            private int position;
            @Override public int read(byte[] buffer, int offset, int length) throws IOException {
                Objects.checkFromIndexSize(offset, length, buffer.length);
                if (length == 0) return 0;
                if (position == line.length) {
                    prompt("null");
                    String next = commands.readLine();
                    if (next == null) return -1;
                    line = (next + "\n").getBytes(StandardCharsets.UTF_8); position = 0;
                }
                int size = Math.min(length, line.length - position);
                System.arraycopy(line, position, buffer, offset, size); position += size;
                return size;
            }
            @Override public int read() throws IOException {
                byte[] one = new byte[1];
                return read(one, 0, 1) == -1 ? -1 : one[0] & 255;
            }
        });
        try (var lock = SaveSessionLock.acquire()) {
            if (lock == null) throw new IOException("This archive is already open.");
            Language.load();
            new GameSession(this::trial).run();
        } catch (Exception failure) {
            System.out.println(Language.isChinese() ? "\n运行已停止，刷新后可读取最近检查点。" : "\nSession stopped. Reload to resume the last checkpoint.");
            // Details go to server-side stderr, never to the visitor's page.
            failure.printStackTrace(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        } finally {
            drain(); protocol.println("E"); protocol.flush();
        }
    }

    private void drain() {
        System.out.flush();
        if (text.size() > 0) {
            protocol.println("T\t" + Base64.getEncoder().encodeToString(text.toByteArray()));
            text.reset();
        }
    }

    private void prompt(String puzzle) {
        drain();
        protocol.println("W\t" + (Language.isChinese() ? "zh" : "en") + "\t" + puzzle);
        protocol.flush();
    }

    PuzzleEncounter.Result trial(Random random) {
        PuzzleBoard board = PuzzleLayouts.generate(random);
        while (!board.finished()) {
            prompt(snapshot(board));
            String input;
            try { input = commands.readLine(); }
            catch (IOException e) { throw new GameInterruptedException(); }
            if (input == null) throw new GameInterruptedException();
            if (input.equals("QUIT")) return PuzzleEncounter.Result.DEFEAT;
            PuzzleBoard.Direction direction = switch (input.toUpperCase(Locale.ROOT)) {
                case "W", "UP" -> PuzzleBoard.Direction.UP;
                case "S", "DOWN" -> PuzzleBoard.Direction.DOWN;
                case "A", "LEFT" -> PuzzleBoard.Direction.LEFT;
                case "D", "RIGHT" -> PuzzleBoard.Direction.RIGHT;
                default -> null;
            };
            if (direction != null) board.move(direction);
        }
        // The final shattered board must be visible before normal game prompts resume.
        prompt(snapshot(board));
        try { if (commands.readLine() == null) throw new GameInterruptedException(); }
        catch (IOException e) { throw new GameInterruptedException(); }
        return board.won() ? PuzzleEncounter.Result.VICTORY : PuzzleEncounter.Result.DEFEAT;
    }

    static String snapshot(PuzzleBoard board) {
        PuzzleBoard.State state = board.state();
        String targets = board.targets().stream().map(p -> Integer.toString(p.y * PuzzleBoard.SIZE + p.x))
                .sorted().collect(java.util.stream.Collectors.joining(","));
        var broken = board.shattered();
        return "{\"size\":" + PuzzleBoard.SIZE + ",\"moves\":" + board.moves() + ",\"limit\":" + PuzzleBoard.MOVE_LIMIT
                + ",\"player\":" + state.player() + ",\"monster\":" + state.monster()
                + ",\"boxes\":[" + state.boxA() + "," + state.boxB() + "],\"targets\":[" + targets + "]"
                + ",\"shattered\":" + (broken == null ? -1 : broken.y * PuzzleBoard.SIZE + broken.x)
                + ",\"finished\":" + board.finished() + ",\"won\":" + board.won() + "}";
    }
}

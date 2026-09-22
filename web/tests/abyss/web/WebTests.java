package abyss.web;

import abyss.puzzle.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.*;

/** Real HTTP, real child JVMs and temporary saves. Never touches desktop/player archives. */
public final class WebTests {
    private static int assertions;
    private static URI origin;
    private static HttpClient client;
    private static Path directory;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static HttpClient browser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
    }
    private static HttpResponse<String> call(String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(origin.resolve(path)).timeout(Duration.ofSeconds(10));
        if (body != null) request.header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private static String ready() throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            var response = call("/api/state", null);
            check(response.statusCode() == 200, "state reachable: " + response.statusCode() + " " + response.body());
            if (response.body().contains("\"ready\":true") || response.body().contains("\"ended\":true")) return response.body();
            Thread.sleep(25);
        }
        throw new AssertionError("Game stopped making progress");
    }
    private static long revision(String state) {
        return Long.parseLong(state.substring(state.indexOf(":") + 1, state.indexOf(",")));
    }
    private static String choose(String text) throws Exception {
        String state = ready();
        check(call("/api/input", revision(state) + "\n" + text).statusCode() == 200, "choice accepted: " + text);
        return ready();
    }

    public static void main(String[] args) throws Exception {
        directory = Files.createTempDirectory("abyss-web-test-");
        try (WebServer server = new WebServer(new InetSocketAddress("127.0.0.1", 0),
                Path.of("web/public"), directory, 4, false, "")) {
            server.start(); origin = URI.create("http://127.0.0.1:" + server.port()); client = browser();
            check(call("/", null).body().contains("ABYSS EXPEDITION · 2.0"), "unified 2.0 homepage delivered");
            check(call("/legacy/", null).body().contains("id=\"board\""), "legacy server-save game remains available");
            String betaPage = call("/realtime-test/", null).body();
            check(betaPage.contains("2.2.6-guard-road"), "guard-road page uses one cache-busted release");
            check(betaPage.contains("battle-road"), "combat scene includes the shared battle road");
            check(betaPage.contains("abyss-scenery"), "authored abyss scenery layer is present");
            check(!betaPage.contains("story-banner"), "story beta is absent from release battle");
            var betaScripts = java.util.regex.Pattern.compile("<script\\s+src=\"([^\"]+)\"").matcher(betaPage);
            int betaScriptCount = 0;
            while (betaScripts.find()) {
                String script = betaScripts.group(1);
                var asset = call(script, null);
                check(asset.statusCode() == 200, "beta script delivered: " + script);
                check(asset.headers().firstValue("Content-Type").orElse("").contains("javascript"), "beta script MIME type: " + script);
                check(!asset.body().isBlank(), "beta script is not empty: " + script);
                betaScriptCount++;
            }
            check(betaScriptCount == 5, "all five beta modules referenced");
            check(call("/assets/backgrounds/dark-theme-cc0.png", null).headers().firstValue("Content-Type").orElse("").equals("image/png"), "CC0 combat background delivered");
            for (String hero : List.of("warrior", "mage", "ranger", "paladin", "necromancer", "creator")) {
                var art = call("/assets/characters/" + hero + ".png", null);
                check(art.statusCode() == 200, "character art delivered: " + hero);
                check(art.headers().firstValue("Content-Type").orElse("").equals("image/png"), "character art MIME type: " + hero);
                check(art.body().length() > 1000, "character art is not empty: " + hero);
            }
            String artPage = call("/art-test/", null).body();
            check(artPage.contains("ART FX LAB"), "art and combat FX lab delivered");
            check(artPage.contains("id=\"story-beta\""), "story beta lives in the test gallery");
            check(call("/art-test/style.css?v=0.5.0", null).headers().firstValue("Content-Type").orElse("").contains("text/css"), "art lab stylesheet MIME type");
            check(call("/art-test/actors.js?v=0.5.0", null).headers().firstValue("Content-Type").orElse("").contains("javascript"), "separate character catalog MIME type");
            check(call("/art-test/rig.js?v=0.6.0", null).headers().firstValue("Content-Type").orElse("").contains("javascript"), "reusable rig runtime MIME type");
            check(call("/art-test/lab.js?v=0.5.0", null).headers().firstValue("Content-Type").orElse("").contains("javascript"), "art lab action engine MIME type");
            check(call("/assets/animation/skeletons/humanoid-heavy.json", null).headers().firstValue("Content-Type").orElse("").contains("application/json"), "skeleton model delivered as JSON");
            check(call("/assets/animation/actions/greatsword-v1.json", null).body().contains("\"heavy\""), "greatsword action set includes heavy attack");
            check(call("/assets/animation/characters/warrior-initial.json", null).body().contains("humanoid-heavy"), "warrior model binds the reusable skeleton");
            check(call("/assets/enemies/iron-golem-test.png", null).headers().firstValue("Content-Type").orElse("").equals("image/png"), "production test monster art delivered");
            check(call("/src/gameBody.java", null).statusCode() == 404, "source not exposed");
            check(call("/../saves/abyss-expedition.save", null).statusCode() == 404, "no path traversal");
            check(call("/api/state", null).statusCode() == 401, "state requires player identity");
            var foreign = HttpRequest.newBuilder(origin.resolve("/api/session")).header("Origin", "https://evil.invalid")
                    .POST(HttpRequest.BodyPublishers.ofString("")).build();
            check(client.send(foreign, HttpResponse.BodyHandlers.ofString()).statusCode() == 403, "cross-site rejected");
            var created = call("/api/session", "");
            check(created.statusCode() == 200, "session starts");
            check(created.headers().firstValue("Set-Cookie").orElse("").contains("HttpOnly; SameSite=Strict"), "cookie flags");
            String first = ready(); check(first.contains("THE GATEWAY"), "real main menu");
            long rev = revision(first);
            check(call("/api/input", rev + "\n1\n2").statusCode() == 400, "multiline rejected");
            check(call("/api/input", rev + "\n1").statusCode() == 200, "start new game");
            check(call("/api/input", rev + "\n1").statusCode() == 409, "double click cannot spend a second turn");
            check(ready().contains("EXPEDITION ARCHIVES"), "seed prompt");
            check(choose("424242").contains("adventurer name"), "seed accepted");
            check(choose("Web测试 <script>alert(1)</script>").contains("CHOOSE YOUR DESCENT"), "unicode and literal HTML name accepted");
            String classes = choose("2"); check(classes.contains("CHOOSE YOUR CHAMPION"), "difficulty accepted");
            check(!classes.substring(classes.indexOf("\"screen\":"), classes.indexOf("\"history\":")).contains("[6] Creator"), "creator hidden");
            String hidden = choose("kz");
            String screen = hidden.substring(hidden.indexOf("\"screen\":"), hidden.indexOf("\"history\":"));
            check(screen.contains("[6] Creator") && !screen.contains("[1] Warrior"), "hidden path contains only Creator");
            check(choose("6").contains("Choose your route"), "class selection enters expedition");
            Path player;
            try (var dirs = Files.list(directory)) { player = dirs.filter(Files::isDirectory).findFirst().orElseThrow(); }
            Path save = player.resolve("expeditions/424242.save");
            check(Files.isRegularFile(save), "seed checkpoint created in isolated web directory");
            byte[] checkpoint = Files.readAllBytes(save);
            check(call("/api/session", "").statusCode() == 200, "refresh rejoins same game");
            check(ready().contains("Choose your route"), "refresh retains exact current prompt");
            HttpClient playerOne = client;
            client = browser(); check(call("/api/session", "").statusCode() == 200, "second browser starts");
            String isolated = ready();
            check(isolated.contains("No active expedition") && !isolated.contains("424242"), "players cannot see each other's archives");
            choose("6"); String chinese = choose("2");
            check(chinese.contains("深渊之门"), "Chinese language supported");
            check(!chinese.contains("THE GATEWAY") && !chinese.contains("Language /"), "Chinese history excludes English and bilingual labels");
            choose("6"); String english = choose("1");
            // The language's own native name intentionally remains 简体中文 in every UI language.
            check(!english.contains("深渊之门") && !english.replace("简体中文", "").matches("(?s).*?[\\u3400-\\u9fff].*"),
                    "English history excludes Chinese except the native language name");
            client = playerOne; check(ready().contains("\"language\":\"en\""), "language isolated across JVMs");
            check(call("/api/pause", "").statusCode() == 200, "pause shuts down safely");
            check(Arrays.equals(checkpoint, Files.readAllBytes(save)), "pause does not rewrite checkpoint");
            check(call("/api/session", "").statusCode() == 200, "resume starts");
            check(ready().contains("Continue expedition"), "resume offers stored expedition");
            check(choose("1").contains("Checkpoint restored"), "resume restores save");
            String battle = choose("1"); check(battle.contains("Attack"), "original combat reachable through web");
            String after = choose("2"); check(after.contains("Reality Rend") || after.contains("Choose your route"), "original skills execute");
            var backup = client.send(HttpRequest.newBuilder(origin.resolve("/api/backup")).build(), HttpResponse.BodyHandlers.ofByteArray());
            check(backup.statusCode() == 200, "backup downloaded");
            boolean seedFound = false;
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(backup.body()))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    check(!entry.getName().contains("worker") && !entry.getName().contains(".."), "backup excludes logs and paths");
                    if (entry.getName().equals("expeditions/424242.save")) seedFound = true;
                }
            }
            check(seedFound, "backup contains seed archive");
            byte[] beforeImport = Files.readAllBytes(save);
            var badImport = client.send(HttpRequest.newBuilder(origin.resolve("/api/import"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[]{1,2,3})).build(), HttpResponse.BodyHandlers.ofString());
            check(badImport.statusCode() == 400, "invalid import rejected");
            check(Arrays.equals(beforeImport, Files.readAllBytes(save)), "invalid import cannot change existing saves");
            var restore = client.send(HttpRequest.newBuilder(origin.resolve("/api/import"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(backup.body())).build(), HttpResponse.BodyHandlers.ofString());
            check(restore.statusCode() == 200, "valid backup imports into separate profile");
            check(ready().contains("Continue expedition"), "imported save offered");
            check(choose("1").contains("Checkpoint restored"), "import restores original Java checkpoint");
        }
        check(Json.quote("\"\n\\\u0000🗝").equals("\"\\\"\\n\\\\\\u0000\\ud83d\\udddd\""), "JSON escapes control and surrogate pairs");
        for (int seed = 0; seed < 12; seed++) {
            PuzzleBoard board = PuzzleLayouts.generate(new Random(seed));
            String snapshot = WebGame.snapshot(board);
            check(snapshot.contains("\"size\":8") && snapshot.contains("\"limit\":40"), "web keeps 8x8 and 40 moves");
            for (var direction : PuzzleLayouts.solve(board, 200000)) board.move(direction);
            check(board.won(), "shared puzzle solvable");
            check(WebGame.snapshot(board).contains("\"targets\":[]"), "broken cores removed, not just animated");
        }
        trialProtocol();
        System.out.println("PASS: " + assertions + " web assertions; HTTP, gameplay, saves, isolation and puzzle rules. Test data: " + directory);
    }

    public static class TrialWorker {
        public static void main(String[] args) {
            System.out.println("RESULT:" + new WebGame().trial(new Random(7)));
        }
    }
    private static void trialProtocol() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process worker = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), TrialWorker.class.getName()).start();
        try (var commands = worker.outputWriter(); var messages = worker.inputReader()) {
            PuzzleBoard board = PuzzleLayouts.generate(new Random(7));
            var path = PuzzleLayouts.solve(board, 200000);
            check(!path.isEmpty(), "trial protocol has winning path");
            for (var step : path) {
                String prompt = messages.readLine();
                check(prompt.equals("W\ten\t" + WebGame.snapshot(board)), "browser receives exact shared board");
                commands.write(step.name()); commands.newLine(); commands.flush(); board.move(step);
            }
            String last = messages.readLine();
            check(last.contains("\"won\":true") && last.contains("\"targets\":[]"), "final victory board is sent before settlement");
            commands.newLine(); commands.flush();
            check(messages.readLine().equals("RESULT:VICTORY"), "trial victory returned to game settlement");
        } finally { worker.destroy(); }
    }
}

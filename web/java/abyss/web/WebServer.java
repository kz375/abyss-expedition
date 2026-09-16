package abyss.web;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Dependency-free HTTP host. Each browser owns an opaque cookie and isolated save directory. */
public final class WebServer implements AutoCloseable {
    private static final String COOKIE = "abyss_player";
    private static final Set<String> ASSETS = Set.of("index.html", "app.js", "styles.css", "mark.svg");
    private final HttpServer server;
    private final Path assets, data;
    private final Map<String, GameProcess> games = new HashMap<>();
    private final ExecutorService requests = new ThreadPoolExecutor(4, 16, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128), new ThreadPoolExecutor.CallerRunsPolicy());
    private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor();
    private final SecureRandom random = new SecureRandom();
    private final int maxPlayers;
    private final boolean secureCookie;
    private final String publicOrigin;

    public WebServer(InetSocketAddress address, Path assets, Path data, int maxPlayers,
                     boolean secureCookie, String publicOrigin) throws IOException {
        this.assets = assets.toAbsolutePath(); this.data = data.toAbsolutePath();
        this.maxPlayers = maxPlayers; this.secureCookie = secureCookie;
        this.publicOrigin = publicOrigin;
        if (!Files.isRegularFile(this.assets.resolve("index.html"))) throw new IOException("Web assets not found: " + assets);
        Files.createDirectories(this.data);
        server = HttpServer.create(address, 64);
        server.setExecutor(requests);
        server.createContext("/", this::handle);
        cleanup.scheduleWithFixedDelay(this::expire, 1, 1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws Exception {
        String host = env("ABYSS_HOST", "127.0.0.1");
        int port = Integer.parseInt(env("PORT", "8080"));
        WebServer app = new WebServer(new InetSocketAddress(host, port),
                Path.of(env("ABYSS_WEB_ASSETS", "web/public")), Path.of(env("ABYSS_WEB_DATA", "web-data")),
                Integer.parseInt(env("ABYSS_MAX_PLAYERS", "8")), Boolean.parseBoolean(env("ABYSS_SECURE_COOKIE", "false")),
                env("ABYSS_PUBLIC_ORIGIN", ""));
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        app.start();
        System.out.println("Abyss Expedition web: http://" + host + ":" + app.port());
        System.out.println("Web saves: " + app.data);
    }

    private static String env(String name, String fallback) { return System.getenv().getOrDefault(name, fallback); }
    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            Headers headers = exchange.getResponseHeaders();
            headers.set("X-Content-Type-Options", "nosniff");
            headers.set("Referrer-Policy", "no-referrer");
            headers.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
            headers.set("Cache-Control", "no-store");
            String path = exchange.getRequestURI().getPath(), method = exchange.getRequestMethod();
            if (path.equals("/healthz") && method.equals("GET")) { send(exchange, 200, "application/json", "{\"ok\":true}"); return; }
            if (!path.startsWith("/api/")) {
                if (!method.equals("GET")) { error(exchange, 405, "Method not allowed"); return; }
                String file = path.equals("/") ? "index.html" : path.substring(1);
                if (!ASSETS.contains(file)) { error(exchange, 404, "Not found"); return; }
                String type = file.endsWith(".js") ? "text/javascript" : file.endsWith(".css") ? "text/css" : file.endsWith(".svg") ? "image/svg+xml" : "text/html";
                send(exchange, 200, type, Files.readAllBytes(assets.resolve(file))); return;
            }
            if (!sameOrigin(exchange)) { error(exchange, 403, "Cross-site request rejected"); return; }
            if (!Set.of("GET", "POST").contains(method)) { error(exchange, 405, "Method not allowed"); return; }
            String id = playerId(exchange);
            if (path.equals("/api/session") && method.equals("POST")) {
                synchronized (games) {
                    if ((id == null || !games.containsKey(id)) && games.size() >= maxPlayers) {
                        error(exchange, 503, "Server busy / 当前玩家已满，请稍后重试。"); return;
                    }
                    if (id == null) id = createProfile();
                    GameProcess game = games.get(id);
                    if (game == null) {
                        if (games.size() >= maxPlayers) { error(exchange, 503, "Server busy / 当前玩家已满，请稍后重试。"); return; }
                        game = new GameProcess(data.resolve(id)); games.put(id, game);
                    }
                    cookie(exchange, id);
                    send(exchange, 200, "application/json", game.snapshot());
                }
                return;
            }
            if (id == null) { error(exchange, 401, "Reconnect to continue / 请重新连接。"); return; }
            if (path.equals("/api/import") && method.equals("POST")) {
                byte[] archive = readLimited(exchange.getRequestBody(), 8_000_000);
                Path staging = Files.createTempDirectory(data, ".import-");
                try {
                    ArchiveImport.unpack(archive, staging);
                    synchronized (games) {
                        if (!games.containsKey(id) && games.size() >= maxPlayers) { error(exchange, 503, "Server busy"); return; }
                        String restoredId = createProfile();
                        Path restored = data.resolve(restoredId);
                        // Move only validated files into a new profile; never overwrite a player's saves.
                        try (var files = Files.list(staging)) {
                            for (Path file : files.toList()) Files.move(file, restored.resolve(file.getFileName()));
                        }
                        GameProcess replacement = new GameProcess(restored);
                        GameProcess previous = games.remove(id);
                        if (previous != null) previous.close();
                        games.put(restoredId, replacement); cookie(exchange, restoredId);
                        send(exchange, 200, "application/json", replacement.snapshot());
                    }
                } catch (IOException e) { error(exchange, 400, "Backup is invalid or incompatible; current archive unchanged / 备份无效或不兼容，原档案未改动。"); }
                finally { ArchiveImport.discardStaging(staging); }
                return;
            }
            if (path.equals("/api/backup") && method.equals("GET")) {
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=abyss-expedition-backup.zip");
                send(exchange, 200, "application/zip", backup(data.resolve(id))); return;
            }
            GameProcess game;
            synchronized (games) { game = games.get(id); }
            if (game == null) { error(exchange, 410, "Session rested. Reconnect to load your checkpoint / 会话已休眠，请重新连接读取检查点。"); return; }
            if (path.equals("/api/state") && method.equals("GET")) { send(exchange, 200, "application/json", game.snapshot()); return; }
            if (path.equals("/api/input") && method.equals("POST")) {
                String body = new String(readLimited(exchange.getRequestBody(), 8192), StandardCharsets.UTF_8);
                int separator = body.indexOf('\n');
                if (separator < 0) { error(exchange, 400, "Missing prompt revision"); return; }
                long revision;
                try { revision = Long.parseLong(body.substring(0, separator)); }
                catch (NumberFormatException e) { error(exchange, 400, "Invalid prompt revision"); return; }
                String input = body.substring(separator + 1);
                if (input.length() > 2000 || input.chars().anyMatch(c -> c < 32 || c == 127)) {
                    error(exchange, 400, "Enter one line, up to 2000 characters / 请输入单行文字。"); return;
                }
                if (!game.submit(revision, input)) { error(exchange, 409, "This turn already changed / 此回合已变化，已刷新。"); return; }
                send(exchange, 200, "application/json", game.snapshot()); return;
            }
            if (path.equals("/api/pause") && method.equals("POST")) {
                synchronized (games) { if (games.get(id) == game) { game.close(); games.remove(id); } }
                send(exchange, 200, "application/json", "{\"paused\":true}"); return;
            }
            if (path.equals("/api/restart") && method.equals("POST")) {
                synchronized (games) {
                    if (games.get(id) != game || !game.ended()) { error(exchange, 409, "Finish or pause the current session first"); return; }
                    game.close(); game = new GameProcess(data.resolve(id)); games.put(id, game);
                    send(exchange, 200, "application/json", game.snapshot());
                }
                return;
            }
            error(exchange, 404, "Not found");
        } catch (TooLarge e) { error(exchange, 413, "Request too large"); }
        catch (IOException | RuntimeException e) {
            System.err.println("Web request failed: " + e.getClass().getSimpleName());
            error(exchange, 500, "Request failed. Your last checkpoint is retained / 请求失败，最近检查点仍保留。");
        } finally { exchange.close(); }
    }

    private boolean sameOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String fetch = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if ("cross-site".equals(fetch)) return false;
        String host = exchange.getRequestHeaders().getFirst("Host");
        if (!publicOrigin.isBlank()) {
            if (!Objects.equals(URI.create(publicOrigin).getRawAuthority(), host)) return false;
            return origin == null || origin.equals(publicOrigin);
        }
        // Local mode rejects DNS rebinding. Public deployment must explicitly set its origin.
        if (!Set.of("localhost:" + port(), "127.0.0.1:" + port(), "[::1]:" + port()).contains(host == null ? "" : host)) return false;
        return origin == null || origin.equals("http://" + host);
    }

    private String playerId(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) return null;
        for (String value : cookies.split(";")) {
            String[] pair = value.trim().split("=", 2);
            if (pair.length == 2 && pair[1].startsWith("\"") && pair[1].endsWith("\"") && pair[1].length() >= 2)
                pair[1] = pair[1].substring(1, pair[1].length() - 1);
            if (pair.length == 2 && pair[0].equals(COOKIE) && pair[1].matches("[a-f0-9]{64}")
                    && Files.isDirectory(data.resolve(pair[1]), LinkOption.NOFOLLOW_LINKS)) return pair[1];
        }
        return null;
    }

    private String createProfile() throws IOException {
        // Bound anonymous disk allocations even when a visitor deliberately drops cookies.
        try (var paths = Files.list(data)) {
            if (paths.limit(1000).count() >= 1000) throw new IOException("Profile capacity reached");
        }
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String id = HexFormat.of().formatHex(bytes);
        Files.createDirectory(data.resolve(id)); return id;
    }

    private void cookie(HttpExchange exchange, String id) {
        exchange.getResponseHeaders().set("Set-Cookie", COOKIE + "=" + id
                + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=31536000" + (secureCookie ? "; Secure" : ""));
    }

    private byte[] backup(Path directory) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes); var files = Files.walk(directory, 2)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String name = directory.relativize(file).toString().replace('\\', '/');
                if (!ArchiveImport.allowed(name)) continue;
                byte[] content;
                try (InputStream input = Files.newInputStream(file)) { content = readLimited(input, 1_000_000); }
                if (bytes.size() + content.length > 16_000_000) throw new TooLarge();
                zip.putNextEntry(new ZipEntry(name)); zip.write(content); zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private static final class TooLarge extends IOException { }
    private static byte[] readLimited(InputStream input, int limit) throws IOException {
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) throw new TooLarge();
        return bytes;
    }
    private static void error(HttpExchange e, int status, String text) throws IOException {
        send(e, status, "application/json", "{\"error\":" + Json.quote(text) + "}");
    }
    private static void send(HttpExchange e, int status, String type, String text) throws IOException {
        send(e, status, type, text.getBytes(StandardCharsets.UTF_8));
    }
    private static void send(HttpExchange e, int status, String type, byte[] content) throws IOException {
        e.getResponseHeaders().set("Content-Type", type + (type.startsWith("text/") || type.equals("application/json") ? "; charset=utf-8" : ""));
        e.sendResponseHeaders(status, content.length);
        e.getResponseBody().write(content);
    }

    private void expire() {
        synchronized (games) {
            Iterator<GameProcess> iterator = games.values().iterator();
            while (iterator.hasNext()) {
                GameProcess game = iterator.next();
                if (System.currentTimeMillis() - game.lastAccess() > TimeUnit.MINUTES.toMillis(30)) { game.close(); iterator.remove(); }
            }
        }
    }
    @Override public void close() {
        cleanup.shutdownNow(); server.stop(1);
        synchronized (games) { games.values().forEach(GameProcess::close); games.clear(); }
        requests.shutdownNow();
    }
}

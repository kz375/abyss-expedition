import javax.tools.*;
import java.nio.file.*;
import java.util.*;

/** java tools/Web.java [test|build]. No dependencies, no changes to the desktop build. */
class Web {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("A full JDK 17+ is required");
        Path build = Path.of("build"); Files.createDirectories(build);
        Path classes = Files.createTempDirectory(build, "web-").toAbsolutePath();
        List<String> options = new ArrayList<>(List.of("--release", "17", "-encoding", "UTF-8", "-d", classes.toString()));
        boolean test = args.length > 0 && args[0].equals("test");
        for (String directory : test ? List.of("src", "web/java", "web/tests") : List.of("src", "web/java")) {
            try (var files = Files.walk(Path.of(directory))) {
                files.filter(p -> p.toString().endsWith(".java")).sorted().forEach(p -> options.add(p.toString()));
            }
        }
        if (compiler.run(null, null, null, options.toArray(String[]::new)) != 0) throw new IllegalStateException("Web compilation failed");
        if (args.length > 0 && args[0].equals("build")) { System.out.println(classes); return; }
        String executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process game = new ProcessBuilder(executable, "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8", "-cp", classes.toString(),
                test ? "abyss.web.WebTests" : "abyss.web.WebServer").inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(game::destroy));
        int result = game.waitFor();
        if (result != 0) throw new IllegalStateException("Web process failed: " + result);
    }
}

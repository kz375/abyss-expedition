import javax.tools.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Run from the project root with: java tools/Build.java (JDK 17+). */
class Build {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null) throw new IllegalStateException("Install a full JDK 17 or newer to build the game.");
        Path root=Path.of("").toAbsolutePath();
        Files.createDirectories(root.resolve("build"));
        Path classes=Files.createTempDirectory(root.resolve("build"),"classes-");
        List<String> options=new ArrayList<>(List.of("--release","17","-encoding","UTF-8","-d",classes.toString()));
        try(var paths=Files.walk(root.resolve("src"))) {
            paths.filter(p->p.toString().endsWith(".java")).sorted().forEach(p->options.add(p.toString()));
        }
        if(compiler.run(null,null,null,options.toArray(String[]::new))!=0) throw new IllegalStateException("Compilation failed");
        Path dist=root.resolve("dist");
        Path desktop=dist.resolve("desktop");
        Files.createDirectories(desktop);
        Manifest manifest=new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION,"1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,"abyss.desktop.DesktopLauncher");
        Path staged=Files.createTempFile(dist,"abyss-", ".jar.tmp");
        try(JarOutputStream jar=new JarOutputStream(Files.newOutputStream(staged),manifest); var files=Files.walk(classes)) {
            for(Path file:files.filter(Files::isRegularFile).sorted().toList()) {
                jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace('\\','/')));
                Files.copy(file,jar); jar.closeEntry();
            }
        }
        Files.move(staged,desktop.resolve("AbyssExpedition.jar"),StandardCopyOption.REPLACE_EXISTING);
        for(String name:List.of("play.command","play.sh","play.cmd")) {
            Path target=desktop.resolve(name);
            Files.copy(root.resolve("launchers").resolve(name),target,StandardCopyOption.REPLACE_EXISTING);
            if(!name.endsWith(".cmd") && !target.toFile().setExecutable(true,false))
                System.out.println("Run with sh if not executable: " + target);
        }
        Files.copy(root.resolve("docs/technical/DESKTOP.md"),desktop.resolve("README.md"),StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Built " + desktop.resolve("AbyssExpedition.jar"));
    }
}

import java.nio.file.*;
import java.util.*;

/** Produces a runtime-bundled image for the CURRENT OS and JDK architecture only. */
class Package {
    public static void main(String[] args) throws Exception {
        if(!Files.isRegularFile(Path.of("dist/AbyssExpedition.jar"))) throw new IllegalStateException("Run java tools/Build.java first");
        boolean windows=System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        Path tool=Path.of(System.getProperty("java.home"),"bin",windows?"jpackage.exe":"jpackage");
        if(!Files.isRegularFile(tool)) throw new IllegalStateException("This JDK does not include jpackage");
        Files.createDirectories(Path.of("releases"));
        Path destination=Files.createTempDirectory(Path.of("releases"),"desktop-").toAbsolutePath();
        List<String> command=List.of(tool.toString(),"--type","app-image","--name","AbyssExpedition",
                "--app-version","1.0.0","--input",Path.of("dist").toAbsolutePath().toString(),
                "--main-jar","AbyssExpedition.jar","--main-class","abyss.desktop.DesktopLauncher",
                "--add-modules","java.desktop","--jlink-options","--strip-debug --no-man-pages --no-header-files",
                "--dest",destination.toString());
        int code=new ProcessBuilder(command).inheritIO().start().waitFor();
        if(code!=0) throw new IllegalStateException("jpackage failed: " + code);
        System.out.println("Bundled desktop image: " + destination + " / " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
    }
}

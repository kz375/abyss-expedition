import javax.tools.*;
import java.nio.file.*;
import java.util.*;

/** Cross-platform test runner. Optional argument: alternate java executable (e.g. bundled runtime). */
class Test {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null) throw new IllegalStateException("A full JDK 17+ is required");
        Path classes=Files.createTempDirectory("abyss-all-tests-");
        List<String> compile=new ArrayList<>(List.of("--release","17","-encoding","UTF-8","-d",classes.toString()));
        for(String directory:List.of("src","tests")) try(var files=Files.walk(Path.of(directory))) {
            files.filter(p->p.toString().endsWith(".java")).sorted().forEach(p->compile.add(p.toString()));
        }
        if(compiler.run(null,null,null,compile.toArray(String[]::new))!=0) throw new IllegalStateException("Test compilation failed");
        String executable=args.length>0?args[0]:Path.of(System.getProperty("java.home"),"bin",
                System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        for(String test:List.of("DesktopTests","RegressionTests","ReviewRegressionTests","CombatLanguageTests","WorldLanguageTests")) {
            int code=new ProcessBuilder(executable,"-Dfile.encoding=UTF-8","-Djava.awt.headless=true",
                    "-cp",classes.toString(),test).inheritIO().start().waitFor();
            if(code!=0) throw new IllegalStateException(test + " failed: " + code);
        }
    }
}

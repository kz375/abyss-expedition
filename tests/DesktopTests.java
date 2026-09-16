import abyss.desktop.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Platform-independent checks; these do not pretend to be native GUI acceptance tests. */
public class DesktopTests {
    private static int checks;
    private static void check(boolean value,String label) { checks++; if(!value) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        Path home=Path.of("example-home").toAbsolutePath();
        check(DesktopPaths.defaultDirectory("Mac OS X",home,null,null).equals(home.resolve("Library/Application Support/AbyssExpedition")),"mac path");
        check(DesktopPaths.defaultDirectory("Windows 11",home,null,null).equals(home.resolve("AppData/Roaming/AbyssExpedition")),"windows fallback path");
        Path roaming=home.resolve("roaming");
        check(DesktopPaths.defaultDirectory("Windows 10",home,roaming.toString(),null).equals(roaming.resolve("AbyssExpedition")),"windows environment path");
        check(DesktopPaths.defaultDirectory("Linux",home,null,null).equals(home.resolve(".local/share/abyss-expedition")),"linux default");
        check(DesktopPaths.defaultDirectory("Linux",home,null,"relative").equals(home.resolve(".local/share/abyss-expedition")),"reject relative XDG");
        check(DesktopPaths.defaultDirectory("Linux",home,null,roaming.toString()).equals(roaming.resolve("abyss-expedition")),"absolute XDG");
        for(Dimension screen: new Dimension[]{new Dimension(800,600),new Dimension(1366,768),new Dimension(3840,2160),new Dimension(320,240)}) {
            Dimension fit=DesktopSizing.fit(new Dimension(1000,780),new Rectangle(screen));
            check(fit.width>0&&fit.height>0&&fit.width<=screen.width&&fit.height<=screen.height,"window fits " + screen);
        }
        LineInput input=new LineInput();
        check(input.submit("中文名字 🦊"),"UTF-8 submission"); check(input.submit(""),"blank line submission");
        BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8));
        check(reader.readLine().equals("中文名字 🦊"),"Unicode roundtrip");
        check(reader.readLine().isEmpty(),"Enter alone preserved");
        check(input.read(new byte[0])==0,"zero length read");
        ExecutorService pool=Executors.newSingleThreadExecutor();
        try {
            Future<Integer> blocked=pool.submit(()->input.read()); input.close();
            check(blocked.get(2,TimeUnit.SECONDS)==-1,"close wakes blocked reader");
        } finally { pool.shutdownNow(); }
        check(input.read()==-1&&!input.submit("late"),"closed input remains EOF");
        LineInput flood=new LineInput();
        for(int i=0;i<64;i++) check(flood.submit("1"),"bounded input available");
        check(!flood.submit("overflow"),"full queue does not block UI"); flood.close();

        Path root=Files.createTempDirectory("abyss-desktop-tests-");
        Path legacy=root.resolve("project/saves"), target=root.resolve("user-data/game");
        Files.createDirectories(legacy.resolve("expeditions"));
        Files.writeString(legacy.resolve("expeditions/42.save"),"snapshot");
        Files.writeString(legacy.resolve("language.properties"),"language=zh");
        Files.writeString(legacy.resolve("session.lock"),"old lock");
        DesktopPaths.importLegacy(legacy,target);
        check(Files.readString(target.resolve("expeditions/42.save")).equals("snapshot"),"legacy archive copied");
        check(Files.exists(legacy.resolve("expeditions/42.save")),"original retained");
        check(!Files.exists(target.resolve("session.lock")),"lock not imported");
        Files.writeString(legacy.resolve("expeditions/42.save"),"different");
        DesktopPaths.importLegacy(legacy,target);
        check(Files.readString(target.resolve("expeditions/42.save")).equals("snapshot"),"existing target never overwritten");
        String prior=System.getProperty("abyss.saveDir");
        try {
            System.setProperty("abyss.saveDir",root.resolve("override").toString()); DesktopPaths.configure();
            check(System.getProperty("abyss.saveDir").equals(root.resolve("override").toString()),"explicit override honored");
        } finally { if(prior==null) System.clearProperty("abyss.saveDir"); else System.setProperty("abyss.saveDir",prior); }
        System.out.println("PASS: " + checks + " desktop compatibility assertions.");
    }
}

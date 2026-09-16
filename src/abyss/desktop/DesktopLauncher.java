package abyss.desktop;

import abyss.core.GameSession;
import abyss.save.SaveSessionLock;
import abyss.ui.Language;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Standalone GUI terminal. Game rules still run on their own non-EDT thread. */
public final class DesktopLauncher {
    private final JFrame frame = new JFrame("Abyss Expedition / 深渊远征");
    private final JTextArea transcript = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton submit = new JButton("Enter / 确定");
    private final LineInput lines = new LineInput();
    private final StringBuilder pending = new StringBuilder();
    private volatile boolean running;
    private javax.swing.Timer drain;

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("A graphical desktop is required. Use the gameBody console entry instead."); return;
        }
        SwingUtilities.invokeLater(() -> {
            try { new DesktopLauncher().open(); }
            catch (Exception e) { JOptionPane.showMessageDialog(null, e.toString(), "Startup failed / 启动失败", JOptionPane.ERROR_MESSAGE); }
        });
    }
    private void open() throws IOException {
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        transcript.setEditable(false); transcript.setLineWrap(true); transcript.setWrapStyleWord(true);
        transcript.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        transcript.setBackground(new Color(15,21,31)); transcript.setForeground(new Color(226,232,239));
        transcript.setMargin(new Insets(12,16,12,16));
        input.setFont(new Font(Font.DIALOG, Font.PLAIN, 17));
        JPanel bottom = new JPanel(new BorderLayout(8,8)); bottom.setBorder(BorderFactory.createEmptyBorder(8,12,12,12));
        bottom.add(new JLabel("Input / 输入"), BorderLayout.WEST); bottom.add(input); bottom.add(submit, BorderLayout.EAST);
        frame.add(new JScrollPane(transcript)); frame.add(bottom, BorderLayout.SOUTH);
        JMenuBar menu = new JMenuBar(); JMenu view = new JMenu("View / 显示"); menu.add(view);
        for (int size : new int[]{14,16,18,22,26}) {
            JMenuItem item = new JMenuItem("Font / 字号 " + size);
            item.addActionListener(e -> transcript.setFont(transcript.getFont().deriveFont((float)size))); view.add(item);
        }
        frame.setJMenuBar(menu);
        ActionListener send = e -> {
            String text = input.getText();
            if (!running || text.length() > 4096 || !lines.submit(text)) { Toolkit.getDefaultToolkit().beep(); return; }
            append(text + "\n"); input.setText(""); input.requestFocusInWindow();
        };
        input.addActionListener(send); submit.addActionListener(send);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (running && JOptionPane.showConfirmDialog(frame,
                        "Close and return to the last saved checkpoint next time?\n关闭后，下次从最近保存的检查点继续；未保存的操作会丢失。",
                        "Leave expedition / 离开冒险", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                lines.close(); drain.stop(); frame.dispose(); System.exit(0);
            }
        });
        PipedInputStream output = new PipedInputStream(65536);
        PrintStream stream = new PrintStream(new PipedOutputStream(output), true, StandardCharsets.UTF_8);
        System.setIn(lines); System.setOut(stream); System.setErr(stream);
        Thread reader = new Thread(() -> {
            try (Reader decoder = new InputStreamReader(output, StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096]; int count;
                while ((count=decoder.read(buffer)) >= 0) synchronized(pending) {
                    pending.append(buffer,0,count);
                    if (pending.length()>200000) pending.delete(0,pending.length()-200000);
                }
            } catch(IOException ignored) { }
        }, "abyss-output"); reader.setDaemon(true); reader.start();
        drain = new javax.swing.Timer(50, e -> {
            String text;
            synchronized(pending) { text=pending.toString(); pending.setLength(0); }
            if (!text.isEmpty()) append(text);
        }); drain.start();
        DesktopSizing.fit(frame,new Dimension(1040,800)); frame.setVisible(true); input.requestFocusInWindow();
        running = true;
        Thread game = new Thread(() -> {
            try {
                DesktopPaths.configure();
                System.out.println("Save directory / 存档目录: " + System.getProperty("abyss.saveDir"));
                try (var lock = SaveSessionLock.acquire()) {
                    if (lock != null) { Language.load(); new GameSession().run(); }
                }
            } catch(Exception e) {
                System.err.println("Game stopped / 游戏已停止: " + e);
                e.printStackTrace();
            } finally {
                running = false; lines.close();
                System.out.println("\nSession ended. You may close this window. / 本次运行结束，可关闭窗口。");
                SwingUtilities.invokeLater(() -> { input.setEnabled(false); submit.setEnabled(false); });
            }
        }, "abyss-game"); game.setDaemon(true); game.start();
    }
    private void append(String text) {
        transcript.append(text);
        int excess = transcript.getDocument().getLength()-200000;
        if (excess>0) transcript.replaceRange("",0,excess);
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }
}

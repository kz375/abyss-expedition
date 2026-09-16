package abyss.puzzle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Presentation and key bindings only; movement and win/loss rules live in PuzzleBoard. */
final class PuzzleWindow extends JPanel {
    private final PuzzleBoard board;
    private final JDialog dialog;
    private final Timer burstTimer;
    private Point burst;
    private boolean closed;
    private PuzzleEncounter.Result result = PuzzleEncounter.Result.UNAVAILABLE;

    PuzzleWindow(PuzzleBoard board) {
        this.board = board;
        dialog = new JDialog((Frame) null, "Abyss Mechanism Trial 8×8 — WASD / Arrow Keys", true);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { abandon(); }
        });
        burstTimer = new Timer(1000, e -> {
            burst = null; repaint();
            if (board.finished()) finish(board.won() ? PuzzleEncounter.Result.VICTORY : PuzzleEncounter.Result.DEFEAT);
        });
        burstTimer.setRepeats(false);
        setPreferredSize(new Dimension(1000, 780));
        setBackground(new Color(8,12,21));
        bind("W", PuzzleBoard.Direction.UP); bind("UP", PuzzleBoard.Direction.UP);
        bind("S", PuzzleBoard.Direction.DOWN); bind("DOWN", PuzzleBoard.Direction.DOWN);
        bind("A", PuzzleBoard.Direction.LEFT); bind("LEFT", PuzzleBoard.Direction.LEFT);
        bind("D", PuzzleBoard.Direction.RIGHT); bind("RIGHT", PuzzleBoard.Direction.RIGHT);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "abandon");
        getActionMap().put("abandon", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { abandon(); }
        });
        dialog.setContentPane(this); dialog.pack();
        abyss.desktop.DesktopSizing.fit(dialog, new Dimension(1000,780));
    }
    void showTrial() { dialog.setVisible(true); }
    PuzzleEncounter.Result result() { return result; }
    void closeUnavailable() { finish(PuzzleEncounter.Result.UNAVAILABLE); }
    private void abandon() { finish(board.won() ? PuzzleEncounter.Result.VICTORY : PuzzleEncounter.Result.DEFEAT); }
    private void bind(String key, PuzzleBoard.Direction direction) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (closed || !board.move(direction)) return;
                if (board.shattered() != null) { burst = board.shattered(); burstTimer.restart(); }
                repaint();
                if (board.finished() && burst == null)
                    finish(board.won() ? PuzzleEncounter.Result.VICTORY : PuzzleEncounter.Result.DEFEAT);
            }
        });
    }
    private void finish(PuzzleEncounter.Result outcome) {
        if (closed) return;
        closed = true; result = outcome; burstTimer.stop(); dialog.dispose();
    }
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        PuzzleArtwork.paint((Graphics2D) g, getWidth(), getHeight(), board.player(), board.monster(),
                board.boxes(), board.targets(), burst, board.moves(), board.cores());
    }
}

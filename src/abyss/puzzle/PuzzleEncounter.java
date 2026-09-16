package abyss.puzzle;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import javax.swing.SwingUtilities;

/** Console/GUI boundary. Window failures never strand the game waiting for a latch. */
public final class PuzzleEncounter {
    public enum Result { VICTORY, DEFEAT, UNAVAILABLE }
    private PuzzleEncounter() { }
    public static boolean play() { return play(new Random()); }
    public static boolean play(Random random) { return playResult(random) == Result.VICTORY; }
    public static Result playResult(Random random) {
        if (GraphicsEnvironment.isHeadless()) return Result.UNAVAILABLE;
        if (SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Launch the puzzle from the game thread, not the UI thread");
        PuzzleBoard board = PuzzleLayouts.generate(random);
        PuzzleWindow[] window = new PuzzleWindow[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                window[0] = new PuzzleWindow(board);
                window[0].showTrial();
            });
            return window[0].result();
        } catch (InterruptedException e) {
            SwingUtilities.invokeLater(() -> { if (window[0] != null) window[0].closeUnavailable(); });
            Thread.currentThread().interrupt();
            return Result.UNAVAILABLE;
        } catch (InvocationTargetException e) {
            SwingUtilities.invokeLater(() -> { if (window[0] != null) window[0].closeUnavailable(); });
            System.err.println("The trial window could not open: " + e.getCause());
            return Result.UNAVAILABLE;
        }
    }
}

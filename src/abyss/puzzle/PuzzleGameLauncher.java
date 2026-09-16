package abyss.puzzle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.Random;

/** Standalone entry point for testing the push-box mechanism game directly. */
public final class PuzzleGameLauncher
{
    private PuzzleGameLauncher() { }

    public static void main(String[] args) throws Exception
    {
        PuzzleEncounter.Result result = PuzzleEncounter.playResult(new Random());
        if (result == PuzzleEncounter.Result.UNAVAILABLE) {
            System.out.println("The trial requires a graphical desktop. No penalty applied.");
            return;
        }
        SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null,
                result == PuzzleEncounter.Result.VICTORY ? "Mechanism monster defeated!" : "Trial failed.",
                "Abyss Mechanism Trial", JOptionPane.INFORMATION_MESSAGE));
    }
}

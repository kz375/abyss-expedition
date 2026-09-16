package abyss.ui;

import abyss.core.GameInterruptedException;

import java.util.Scanner;

/** Input boundary for terminal gameplay. */
public final class ConsoleInput
{
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner)
    {
        this.scanner = scanner;
    }

    public boolean hasNextLine() { return scanner.hasNextLine(); }

    public String nextLine()
    {
        if (!scanner.hasNextLine()) throw new GameInterruptedException();
        return scanner.nextLine();
    }

    public int readChoice(int minimum, int maximum)
    {
        while (true)
        {
            System.out.print("> ");
            String text = nextLine().trim();
            try
            {
                int choice = Integer.parseInt(text);
                if (choice >= minimum && choice <= maximum) return choice;
            }
            catch (NumberFormatException ignored)
            {
            }
            System.out.println(Language.t("Enter a number between ") + minimum + Language.t(" and ") + maximum + ".");
        }
    }

    public void close() { scanner.close(); }
}

package abyss.ui;

/** Small, reusable terminal headers for the expedition flow. */
public final class ConsoleHeaders
{
    private ConsoleHeaders() { }

    public static void floor(int floor, int finalFloor)
    {
        String title = abyss.ui.Language.t("ABYSS FLOOR ") + floor + " / " + finalFloor;
        System.out.println();
        System.out.println("  ──────────────────────────── ❉ ────────────────────────────");
        System.out.println(ConsoleLayout.centered(title, 68));
        System.out.println("  ────────────────────────────────────────────────────────────");
    }
}

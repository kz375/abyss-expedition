package abyss.ui;

/** Reusable formatting helpers for the terminal interface. */
public final class ConsoleLayout
{
    private ConsoleLayout()
    {
    }

    public static String spaces(int count)
    {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++)
        {
            builder.append(' ');
        }
        return builder.toString();
    }

    public static String centered(String text, int width)
    {
        int padding = Math.max(0, (width - displayWidth(text)) / 2);
        return spaces(padding) + text;
    }

    /** Approximate terminal cells for Latin/CJK text; do not alter player names. */
    public static int displayWidth(String text) {
        return text.codePoints().map(c -> (c >= 0x2E80 && c <= 0xA4CF)
                || (c >= 0xAC00 && c <= 0xD7AF) || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFE10 && c <= 0xFE6F) || (c >= 0xFF01 && c <= 0xFF60)
                || (c >= 0x1F300 && c <= 0x1FAFF) || (c >= 0x20000 && c <= 0x3FFFF) ? 2 : 1).sum();
    }
    public static String padded(String text, int width) {
        return text + spaces(Math.max(0, width - displayWidth(text)));
    }
}

package abyss.content;

import java.util.Random;

/** Extra combat identity granted to elite enemies. */
public enum EliteAffix
{
    NONE(""),
    ARMORED("Armored"),
    FRENZIED("Frenzied");

    private final String label;

    EliteAffix(String label)
    {
        this.label = label;
    }

    public String label()
    {
        return label;
    }

    public static EliteAffix random(Random random)
    {
        return random.nextBoolean() ? ARMORED : FRENZIED;
    }
}

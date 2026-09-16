package abyss.content;

/** Input service used by non-combat locations. */
public interface LocationServices
{
    int readChoice(int minimum, int maximum);
    int nextInt(int bound);
}

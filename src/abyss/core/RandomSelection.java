package abyss.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Shared random-selection helpers for content generation. */
public final class RandomSelection
{
    private RandomSelection() { }

    public static <T> List<T> sampleDistinct(List<T> pool, int count, Random random)
    {
        List<T> remaining = new ArrayList<>(pool);
        List<T> picked = new ArrayList<>();
        while (picked.size() < count && !remaining.isEmpty())
            picked.add(remaining.remove(random.nextInt(remaining.size())));
        return picked;
    }

    public static <T> T randomElement(List<T> pool, Random random)
    {
        return pool.get(random.nextInt(pool.size()));
    }
}

package abyss.content;

/** Deterministic encounter knowledge. Reading it never consumes expedition RNG. */
public final class Prophecy {
    private Prophecy() { }

    public static String visionForFloor(int floor) {
        return switch (floor) {
            case 1, 2 -> "an early swarm (Cave Bat, Abyss Hound, Lost Miner)";
            case 3 -> "Relic Guardian";
            case 4, 5 -> "a shadow host (Shadow Assassin, Cursed Doll, Bone Scholar)";
            case 6, 7 -> "a lost legion (Void Stalker, Bone Reaper, Dread Wraith)";
            case 8 -> "Abyss Lord";
            default -> "nothing beyond the final gate";
        };
    }
}

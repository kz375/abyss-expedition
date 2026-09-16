package abyss.achievement;

/** Stable enum names are profile keys; achievements never grant combat bonuses. */
public enum Achievement {
    FIRST_BLOOD("First Blood", "Defeat your first enemy."),
    ELITE_HUNTER("Elite Hunter", "Defeat an elite enemy."),
    GUARDIAN_BREAKER("Guardian Breaker", "Defeat a boss."),
    DEEP_DELVER("Deep Delver", "Reach floor 5."),
    RELIC_KEEPER("Relic Keeper", "Hold 5 different relics in one expedition."),
    GOLD_SEEKER("Gold Seeker", "Earn 300 gold in one expedition."),
    BATTLE_VETERAN("Battle Veteran", "Deal 1,000 health damage in one expedition."),
    TWIN_CORES("Twin Cores", "Win the mechanism trial during an expedition."),
    STAR_BEARER("Star Bearer", "Complete an expedition."),
    NIGHTMARE_CONQUEROR("Nightmare Conqueror", "Win on Nightmare or Ultra Nightmare."),
    ABYSS_MASTER("Abyss Master", "Win on Ultra Nightmare.");

    private final String title;
    private final String description;
    Achievement(String title, String description) { this.title = title; this.description = description; }
    public String title() { return title; }
    public String description() { return description; }
}

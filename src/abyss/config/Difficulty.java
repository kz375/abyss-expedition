package abyss.config;

/** Difficulty values kept separate from combat and presentation code. */
public enum Difficulty
{
    EXPLORER("Explorer", 0.85, 0.85, 0.85, 1.0, 1.35, 1.05),
    ADVENTURER("Adventurer", 1.0, 1.0, 1.0, 1.0, 1.35, 1.10),
    NIGHTMARE("Nightmare", 1.3, 1.2, 1.5, 1.0, 1.35, 1.15),
    ULTRA_NIGHTMARE("Ultra Nightmare", 4.0, 2.4, 2.0, 3.7, 1.40, 1.15);

    private final String label;
    private final double enemyHealthMultiplier;
    private final double enemyAttackMultiplier;
    private final double rewardMultiplier;
    private final double heroStatMultiplier;
    private final double eliteRankMultiplier;
    private final double bossRankMultiplier;

    Difficulty(String label, double enemyHealthMultiplier, double enemyAttackMultiplier,
               double rewardMultiplier, double heroStatMultiplier,
               double eliteRankMultiplier, double bossRankMultiplier)
    {
        this.label = label;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.enemyAttackMultiplier = enemyAttackMultiplier;
        this.rewardMultiplier = rewardMultiplier;
        this.heroStatMultiplier = heroStatMultiplier;
        this.eliteRankMultiplier = eliteRankMultiplier;
        this.bossRankMultiplier = bossRankMultiplier;
    }

    public String label()
    {
        return label;
    }

    public double enemyHealthMultiplier()
    {
        return enemyHealthMultiplier;
    }

    public double enemyAttackMultiplier()
    {
        return enemyAttackMultiplier;
    }

    public double rewardMultiplier()
    {
        return rewardMultiplier;
    }

    public double heroStatMultiplier()
    {
        return heroStatMultiplier;
    }

    public double eliteRankMultiplier()
    {
        return eliteRankMultiplier;
    }

    public double bossRankMultiplier()
    {
        return bossRankMultiplier;
    }
}

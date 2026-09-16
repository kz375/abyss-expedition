package abyss.combat;

/** The action an enemy has committed to for its upcoming turn. */
public enum EnemyIntent
{
    ATTACK("Attack"),
    SPECIAL("Special"),
    RECOVERING("Attack (skill recovering)");

    private final String label;

    EnemyIntent(String label)
    {
        this.label = label;
    }

    public String label()
    {
        return label;
    }
}

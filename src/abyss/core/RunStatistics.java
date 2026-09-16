package abyss.core;

/** Mutable statistics for one expedition. Kept outside the game-flow coordinator. */
public final class RunStatistics
{
    private int normalKills;
    private int eliteKills;
    private int bossKills;
    private int damageDealt;
    private int damageTaken;
    private int goldEarned;
    private int potionsDrunk;
    private int eventsResolved;
    private int totalTurns;
    private int floorReached;

    public void addNormalKill() { normalKills++; }
    public void addEliteKill() { eliteKills++; }
    public void addBossKill() { bossKills++; }
    public void addDamageDealt(int amount) { damageDealt += amount; }
    public void addDamageTaken(int amount) { damageTaken += amount; }
    public void addGoldEarned(int amount) { goldEarned += amount; }
    public void addPotionDrunk() { potionsDrunk++; }
    public void addEventResolved() { eventsResolved++; }
    public void addTurn() { totalTurns++; }
    public void setFloorReached(int floor) { floorReached = floor; }
    public int getNormalKills() { return normalKills; }
    public int getEliteKills() { return eliteKills; }
    public int getBossKills() { return bossKills; }
    public int getDamageDealt() { return damageDealt; }
    public int getDamageTaken() { return damageTaken; }
    public int getGoldEarned() { return goldEarned; }
    public int getPotionsDrunk() { return potionsDrunk; }
    public int getEventsResolved() { return eventsResolved; }
    public int getTotalTurns() { return totalTurns; }
    public int getFloorReached() { return floorReached; }

    public void reset()
    {
        normalKills = eliteKills = bossKills = damageDealt = damageTaken = goldEarned = 0;
        potionsDrunk = eventsResolved = totalTurns = floorReached = 0;
    }

    public void restoreCheckpoint(int normalKills, int eliteKills, int bossKills, int damageDealt, int damageTaken,
                                  int goldEarned, int potionsDrunk, int eventsResolved, int totalTurns, int floorReached)
    {
        this.normalKills = normalKills;
        this.eliteKills = eliteKills;
        this.bossKills = bossKills;
        this.damageDealt = damageDealt;
        this.damageTaken = damageTaken;
        this.goldEarned = goldEarned;
        this.potionsDrunk = potionsDrunk;
        this.eventsResolved = eventsResolved;
        this.totalTurns = totalTurns;
        this.floorReached = floorReached;
    }
}

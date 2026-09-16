package abyss.combat;

import java.util.List;

/** Shared contract for every participant in battle. */
public interface Combatant
{
    boolean isAlive();

    String getName();

    int getAttack();

    int getDefense();

    int getShield();

    int getHealth();

    int getMaxHealth();

    boolean isPlayer();

    List<StatusEffect> getStatuses();

    void takeDamage(int amount);

    void restoreHealth(int amount);

    void sufferStatusDamage(int amount);

    default void applyStatus(String name, int turns, int power, boolean stack)
    {
        StatusEffect existing = StatusEffect.find(getStatuses(), name);
        if (existing != null)
        {
            existing.refresh(turns, power, stack);
            return;
        }
        getStatuses().add(StatusEffect.create(name, turns, power));
    }

    default StatusEffect findStatus(String name)
    {
        return StatusEffect.find(getStatuses(), name);
    }

    default int getCurrentAttack()
    {
        StatusEffect weak = findStatus("Weak");
        return Math.max(1, getAttack() - (weak == null ? 0 : weak.getPower()));
    }

    default int getCurrentDefense()
    {
        StatusEffect sunder = findStatus("Sunder");
        return Math.max(0, getDefense() - (sunder == null ? 0 : sunder.getPower()));
    }

    default boolean beginTurn()
    {
        for (StatusEffect status : getStatuses())
        {
            status.startTurn(this);
            if (!isAlive())
            {
                break;
            }
        }
        boolean stunned = false;
        for (StatusEffect status : getStatuses())
        {
            if (status.preventsAction())
            {
                stunned = true;
            }
        }
        return stunned;
    }

    default void endTurn()
    {
        for (StatusEffect status : getStatuses()) status.onTurnEnd(this);
        getStatuses().removeIf(status -> status.getTurns() <= 0);
    }
}

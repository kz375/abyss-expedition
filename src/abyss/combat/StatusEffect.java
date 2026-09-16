package abyss.combat;

import java.util.List;

/** Base type and factory for all temporary combat statuses. */
public abstract class StatusEffect
{
    private final String name;
    private int turns;
    private int power;
    private boolean activeThisTurn;

    protected StatusEffect(String name, int turns, int power)
    {
        this.name = name;
        this.turns = turns;
        this.power = power;
    }

    public String getName()
    {
        return name;
    }

    public int getTurns()
    {
        return turns;
    }

    public int getPower()
    {
        return power;
    }

    public void refresh(int turns, int power, boolean stack)
    {
        this.power = stack ? this.power + power : Math.max(this.power, power);
        this.turns = Math.max(this.turns, turns);
    }

    public abstract void onTurnStart(Combatant owner);

    public final void startTurn(Combatant owner)
    {
        activeThisTurn = true;
        onTurnStart(owner);
    }

    public void onTurnEnd(Combatant owner)
    {
        if (activeThisTurn) turns--;
        activeThisTurn = false;
    }

    public boolean preventsAction()
    {
        return false;
    }

    public static StatusEffect find(List<StatusEffect> statuses, String name)
    {
        for (StatusEffect status : statuses)
        {
            if (status.getName().equals(name))
            {
                return status;
            }
        }
        return null;
    }

    public static StatusEffect create(String name, int turns, int power)
    {
        return switch (name)
        {
            case "Burn" -> new Burn(turns, power);
            case "Poison" -> new Poison(turns, power);
            case "Stun" -> new Stun(turns, power);
            case "Weak" -> new Weak(turns, power);
            case "Regen" -> new Regen(turns, power);
            case "Empower" -> new Empower(turns, power);
            case "Sunder" -> new Sunder(turns, power);
            case "Curse" -> new Curse(turns, power);
            default -> throw new IllegalArgumentException("Unknown status: " + name);
        };
    }

    private static final class Burn extends StatusEffect
    {
        private Burn(int turns, int power) { super("Burn", turns, power); }

        @Override public void onTurnStart(Combatant owner)
        {
            System.out.println((owner.isPlayer() ? abyss.ui.Language.battle("You suffer ") : abyss.ui.Language.t(owner.getName()) + abyss.ui.Language.battle(" suffers "))
                    + getPower() + abyss.ui.Language.battle(" burn damage."));
            owner.sufferStatusDamage(getPower());
        }
    }

    private static final class Poison extends StatusEffect
    {
        private Poison(int turns, int power) { super("Poison", turns, power); }

        @Override public void onTurnStart(Combatant owner)
        {
            System.out.println((owner.isPlayer() ? abyss.ui.Language.battle("You suffer ") : abyss.ui.Language.t(owner.getName()) + abyss.ui.Language.battle(" suffers "))
                    + getPower() + abyss.ui.Language.battle(" poison damage."));
            owner.sufferStatusDamage(getPower());
        }
    }

    private static final class Stun extends StatusEffect
    {
        private Stun(int turns, int power) { super("Stun", turns, power); }
        @Override public void onTurnStart(Combatant owner) { }
        @Override public boolean preventsAction() { return true; }
    }

    private static final class Weak extends StatusEffect
    {
        private Weak(int turns, int power) { super("Weak", turns, power); }
        @Override public void onTurnStart(Combatant owner) { }
    }

    private static final class Regen extends StatusEffect
    {
        private Regen(int turns, int power) { super("Regen", turns, power); }
        @Override public void onTurnStart(Combatant owner)
        {
            owner.restoreHealth(getPower());
            System.out.println(abyss.ui.Language.battle("Regeneration restores ") + getPower() + abyss.ui.Language.battle(" health."));
        }
    }

    private static final class Empower extends StatusEffect
    {
        private Empower(int turns, int power) { super("Empower", turns, power); }
        @Override public void onTurnStart(Combatant owner) { }
    }

    private static final class Sunder extends StatusEffect
    {
        private Sunder(int turns, int power) { super("Sunder", turns, power); }
        @Override public void onTurnStart(Combatant owner) { }
    }

    private static final class Curse extends StatusEffect
    {
        private Curse(int turns, int power) { super("Curse", turns, power); }
        @Override public void onTurnStart(Combatant owner) { }
    }
}

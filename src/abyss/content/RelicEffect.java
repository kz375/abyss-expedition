package abyss.content;

import abyss.combat.Combatant;
import abyss.combat.HeroContext;

/** Extension point for relic effects. Individual relics will be migrated here next. */
public interface RelicEffect
{
    String getName();

    String getDescription();

    default void onEquip(HeroContext hero) { }
    default void onUnequip(abyss.combat.Hero hero) { }

    default void onBattleStart(HeroContext hero, Combatant enemy) { }

    default int modifyOutgoingDamage(HeroContext hero, Combatant enemy, int damage)
    {
        return damage;
    }

    default int modifyIncomingDamage(HeroContext hero, Combatant enemy, int damage)
    {
        return damage;
    }

    default void onBasicAttack(HeroContext hero, Combatant enemy) { }
    default void onDamageDealt(HeroContext hero, Combatant enemy, int healthDamage) { }
    default void onDamageTaken(HeroContext hero, Combatant enemy, int healthDamage) { }
}

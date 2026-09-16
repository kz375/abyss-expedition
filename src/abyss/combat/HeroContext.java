package abyss.combat;

import abyss.content.RelicEffect;

import java.util.List;

/** Operations that combat content may perform on the player hero. */
public interface HeroContext extends Combatant
{
    int getLevel();

    int getCritBonus();

    int getPotionBonusPct();

    int getGoldBonusPct();

    int getCooldownReduction();

    int getGold();

    int getPotions();

    int getSkillCooldown();

    List<RelicEffect> getEquippedRelics();

    void addShield(int amount);

    void addAttack(int amount);

    void addDefense(int amount);

    void addMaxHealth(int amount);

    void addCritBonus(int amount);

    void addPotionBonusPct(int amount);

    void addGoldBonusPct(int amount);

    void addCooldownReduction(int amount);

    void enablePhoenix();

    void addGold(int amount);

    void spendGold(int amount);

    void receivePotion();

    void recordDamageDealt(int amount);

    int dealDamage(Combatant enemy, double multiplier);

    boolean canStun(Combatant enemy);

    boolean rollChance(int percent);

    int receiveDamageFrom(Combatant enemy, double multiplier);
}

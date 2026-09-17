package abyss.content;

import abyss.combat.Hero;
import abyss.config.Difficulty;

import java.util.ArrayList;
import java.util.List;

/** All random event effects. */
public enum GameEvent
{
    ANCIENT_SHRINE { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("You find an ancient shrine. 1. Offer 15 health for 3 attack  2. Pray to restore 20 health")); if (s.readChoice(1, 2) == 1) { if (h.spendHealth(15)) { h.addAttack(3); System.out.println(abyss.ui.Language.t("The shrine accepts your offering. Attack +3.")); } else System.out.println(abyss.ui.Language.t("The shrine refuses an offering that would cost your life.")); } else { h.restoreHealth(20); System.out.println(abyss.ui.Language.t("A warm light restores 20 health.")); } } },
    LOCKED_CHEST { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("A locked chest blocks your path. 1. Open it  2. Leave it")); if (s.readChoice(1, 2) == 1) { if (s.nextInt(100) < 70) { int gold = s.gainGold(h, 25 + f * 8); System.out.println(abyss.ui.Language.t("The chest contains ") + gold + abyss.ui.Language.t(" gold.")); } else { System.out.println(abyss.ui.Language.t("It was a mimic!")); s.resolveBattle(h, f, true, d); } } else System.out.println(abyss.ui.Language.t("You leave the chest untouched.")); } },
    WANDERING_HEALER { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("A wandering healer offers a choice. 1. Buy a potion for 12 gold  2. Accept a risky blessing")); if (s.readChoice(1, 2) == 1) { if (h.getGold() >= 12) { h.spendGold(12); h.receivePotion(); System.out.println(abyss.ui.Language.t("You receive a potion.")); } else System.out.println(abyss.ui.Language.t("Not enough gold. The healer leaves.")); } else if (s.nextBoolean()) { h.addDefense(2); System.out.println(abyss.ui.Language.t("The blessing grants 2 defense.")); } else { h.takeDamage(18); int gold = s.gainGold(h, 35); System.out.println(abyss.ui.Language.t("The blessing hurts, but grants ") + gold + abyss.ui.Language.t(" gold.")); } } },
    MYSTIC_SPRING { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("A mystic spring shimmers with pale light. 1. Drink  2. Leave")); if (s.readChoice(1, 2) == 1) { if (s.nextInt(100) < 55) { h.addMaxHealth(5); h.restoreHealth(40); System.out.println(abyss.ui.Language.t("The water is blessed! Max health +5 and 40 health restored.")); } else { h.takeDamage(16); System.out.println(abyss.ui.Language.t("The water is fouled! You suffer 16 damage.")); } } else System.out.println(abyss.ui.Language.t("You resist the urge and move on.")); } },
    TRAPPED_ADVENTURER { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("An adventurer is caught in a bear trap. 1. Free him  2. Walk away")); if (s.readChoice(1, 2) == 1) { if (s.nextInt(100) < 60) { int gold = s.gainGold(h, 30 + f * 6); h.receivePotion(); System.out.println(abyss.ui.Language.t("He thanks you and gives ") + gold + abyss.ui.Language.t(" gold and a potion.")); } else { System.out.println(abyss.ui.Language.t("It was a bandit in disguise! He attacks!")); s.resolveBattle(h, f, true, d); } } else System.out.println(abyss.ui.Language.t("You avert your eyes and move on. Somewhere behind you, a trap clicks.")); } },
    DICE_GAMBLER { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("A dice gambler grins at you. 1. Bet 15 gold  2. Bet 40 gold  3. Bet 70 gold  4. Walk away")); int choice = s.readChoice(1, 4); int stake = choice == 1 ? 15 : choice == 2 ? 40 : choice == 3 ? 70 : 0; if (choice == 4) System.out.println(abyss.ui.Language.t("The gambler shrugs and vanishes into the dark.")); else if (h.getGold() < stake) System.out.println(abyss.ui.Language.t("Not enough gold. The gambler laughs at you.")); else { int you = 1 + s.nextInt(6), gambler = 1 + s.nextInt(6); System.out.println(abyss.ui.Language.t("You roll ") + you + abyss.ui.Language.t(" vs the gambler's ") + gambler + "."); if (you > gambler) { int gold = s.gainGold(h, stake); System.out.println(abyss.ui.Language.t("You win ") + gold + abyss.ui.Language.t(" gold!")); } else if (you < gambler) { h.spendGold(stake); System.out.println(abyss.ui.Language.t("You lose ") + stake + abyss.ui.Language.t(" gold.")); } else System.out.println(abyss.ui.Language.t("A tie. The gambler returns your stake.")); } } },
    ANCIENT_LIBRARY { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("An ancient library slumbers in the dark. 1. Study combat tomes (+1 attack)  2. Meditate (+1 defense)  3. Leave")); int choice = s.readChoice(1, 3); if (choice == 1) { h.addAttack(1); System.out.println(abyss.ui.Language.t("Hours of study sharpen your blade. Attack +1.")); } else if (choice == 2) { h.addDefense(1); System.out.println(abyss.ui.Language.t("Meditation hardens your resolve. Defense +1.")); } else System.out.println(abyss.ui.Language.t("You leave the dusty shelves behind.")); } },
    WHISPERING_WELL { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("A stone well whispers your name. 1. Toss 25 gold into it  2. Leave")); if (s.readChoice(1, 2) != 1) { System.out.println(abyss.ui.Language.t("The whispers fade as you leave.")); return; } if (h.getGold() < 25) { System.out.println(abyss.ui.Language.t("You lack the coin. The whispers fade, disappointed.")); return; } h.spendGold(25); int roll = s.nextInt(100); if (roll < 45) { List<RelicEffect> unowned = s.unownedRelics(h); if (unowned.isEmpty()) { h.addAttack(2); System.out.println(abyss.ui.Language.t("The well grants +2 attack.")); } else { System.out.println(abyss.ui.Language.t("Something rises from the well...")); h.equipRelic(s.randomElement(unowned)); } } else if (roll < 70) { h.addAttack(2); System.out.println(abyss.ui.Language.t("The whispers teach you fury. Attack +2.")); } else if (roll < 85) { h.addDefense(2); System.out.println(abyss.ui.Language.t("The whispers harden your skin. Defense +2.")); } else { h.addMaxHealth(20); h.restoreHealth(20); System.out.println(abyss.ui.Language.t("Vitality surges through you. Max health +20 and 20 health restored.")); } } },
    CARD_SHARP { public void resolve(EventServices s, Hero h, int f, Difficulty d) { System.out.println(abyss.ui.Language.t("A masked card sharp offers a wager. 1. Pay 25 gold for a card  2. Leave")); if (s.readChoice(1, 2) != 1) { System.out.println(abyss.ui.Language.t("The cards vanish into the dark.")); return; } if (h.getGold() < 25) { System.out.println(abyss.ui.Language.t("Not enough gold. The card sharp laughs.")); return; } h.spendGold(25); int roll = s.nextInt(100); if (roll < 45) { int gold = s.gainGold(h, 60); System.out.println(abyss.ui.Language.t("A golden card! You receive ") + gold + abyss.ui.Language.t(" gold.")); } else if (roll < 80) { h.receivePotion(); h.addShield(20); System.out.println(abyss.ui.Language.t("A silver card grants a potion and 20 shield.")); } else { h.takeDamage(14); System.out.println(abyss.ui.Language.t("A cursed card cuts your hand. You suffer 14 damage.")); } } },
    ORACLE { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        System.out.println(abyss.ui.Language.t("A blind oracle traces circles in ash. 1. Foresee the next floor  2. Foresee two floors ahead  3. Leave"));
        int choice = s.readChoice(1, 3);
        if (choice == 3) { System.out.println(abyss.ui.Language.t("The oracle lets the ash scatter in silence.")); return; }
        int target = f + choice;
        System.out.println(abyss.ui.Language.t("The oracle sees Abyss Floor ") + target + abyss.ui.Language.t(": ")
                + abyss.ui.Language.t(Prophecy.visionForFloor(target)) + ".");
    } },
    RELIC_CURATOR { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        int cost = 55 + f * 10;
        System.out.println(abyss.ui.Language.t("A relic curator unlocks a velvet case. 1. Pay ") + cost
                + abyss.ui.Language.t(" gold to examine three relics  2. Leave"));
        if (s.readChoice(1, 2) != 1) { System.out.println(abyss.ui.Language.t("The curator closes the case and bows.")); return; }
        if (h.getGold() < cost) { System.out.println(abyss.ui.Language.t("Not enough gold. The curator keeps the case sealed.")); return; }
        List<RelicEffect> remaining = new ArrayList<>(s.unownedRelics(h));
        if (remaining.isEmpty()) { System.out.println(abyss.ui.Language.t("The curator finds no relic you do not already own.")); return; }
        h.spendGold(cost);
        List<RelicEffect> offers = new ArrayList<>();
        while (offers.size() < Math.min(3, remaining.size())) offers.add(remaining.remove(s.nextInt(remaining.size())));
        System.out.println(abyss.ui.Language.t("Choose one relic from the curator's case:"));
        for (int index = 0; index < offers.size(); index++) System.out.println("  " + (index + 1) + ". "
                + abyss.ui.Language.t(offers.get(index).getName()) + " - " + abyss.ui.Language.t(offers.get(index).getDescription()));
        h.equipRelic(offers.get(s.readChoice(1, offers.size()) - 1));
    } },
    RIFT_GATE { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        System.out.println(abyss.ui.Language.t("A rift gate hums with an elite presence. 1. Enter the rift  2. Leave"));
        if (s.readChoice(1, 2) != 1) { System.out.println(abyss.ui.Language.t("The rift folds shut behind you.")); return; }
        System.out.println(abyss.ui.Language.t("You step through the rift. An elite guardian is waiting."));
        s.resolveBattle(h, f, true, d);
    } },
    FORGOTTEN_FORGE { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        System.out.println(abyss.ui.Language.t("A forgotten forge still burns. 1. Temper your weapon (lose 12 health, gain 4 attack)  2. Reinforce your armor (lose 12 health, gain 4 defense)  3. Leave"));
        int choice = s.readChoice(1, 3);
        if (choice == 3) { System.out.println(abyss.ui.Language.t("You leave the forge to its endless work.")); return; }
        if (!h.spendHealth(12)) { System.out.println(abyss.ui.Language.t("You are too wounded to work the forge.")); return; }
        if (choice == 1) { h.addAttack(4); System.out.println(abyss.ui.Language.t("The forge tempers your weapon. Attack +4.")); }
        else { h.addDefense(4); System.out.println(abyss.ui.Language.t("The forge reinforces your armor. Defense +4.")); }
    } },
    ECHOING_ALTAR { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        System.out.println(abyss.ui.Language.t("An echoing altar answers your pulse. 1. Offer 30 gold for vitality  2. Listen for a ward  3. Leave"));
        int choice = s.readChoice(1, 3);
        if (choice == 1) {
            if (h.getGold() < 30) { System.out.println(abyss.ui.Language.t("The altar falls silent. You lack the required gold.")); return; }
            h.spendGold(30); h.addMaxHealth(14); h.restoreHealth(20);
            System.out.println(abyss.ui.Language.t("The altar grants vitality. Max health +14 and 20 health restored."));
        } else if (choice == 2) { h.addShield(30 + f * 3); System.out.println(abyss.ui.Language.t("A resonant ward forms around you.")); }
        else System.out.println(abyss.ui.Language.t("The echoes fade as you walk away."));
    } },
    MOONLIT_CARAVAN { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        int cost = 20 + f * 3;
        System.out.println(abyss.ui.Language.t("A moonlit caravan offers rare supplies. 1. Pay ") + cost
                + abyss.ui.Language.t(" gold for a potion and a ward  2. Leave"));
        if (s.readChoice(1, 2) != 1) { System.out.println(abyss.ui.Language.t("The caravan fades into the mist.")); return; }
        if (h.getGold() < cost) { System.out.println(abyss.ui.Language.t("The caravan master shakes their head. Not enough gold.")); return; }
        h.spendGold(cost); h.receivePotion(); h.addShield(18 + f * 3);
        System.out.println(abyss.ui.Language.t("You receive a potion and a moonlit ward."));
    } },
    STARVED_IDOL { public void resolve(EventServices s, Hero h, int f, Difficulty d) {
        System.out.println(abyss.ui.Language.t("A starved idol opens its stone mouth. 1. Feed it 10 health for 45 gold  2. Leave"));
        if (s.readChoice(1, 2) != 1) { System.out.println(abyss.ui.Language.t("The idol closes its mouth and goes still.")); return; }
        if (!h.spendHealth(10)) { System.out.println(abyss.ui.Language.t("The idol refuses a life-threatening offering.")); return; }
        int gold = s.gainGold(h, 45 + f * 5);
        System.out.println(abyss.ui.Language.t("The idol spits out ") + gold + abyss.ui.Language.t(" gold."));
    } },
    MYSTERIOUS_STALKER { public void resolve(EventServices s, Hero h, int f, Difficulty d) { s.resolveMechanismEncounter(h, f, d); } };

    public abstract void resolve(EventServices services, Hero hero, int floor, Difficulty difficulty);

    public static void resolveRandom(EventServices services, Hero hero, int floor, Difficulty difficulty)
    {
        if (floor >= 5 && services.canEncounterMechanism() && services.nextInt(100) < 5)
        {
            MYSTERIOUS_STALKER.resolve(services, hero, floor, difficulty);
            return;
        }
        GameEvent[] regularEvents = {
                ANCIENT_SHRINE, LOCKED_CHEST, WANDERING_HEALER, MYSTIC_SPRING, TRAPPED_ADVENTURER,
                DICE_GAMBLER, ANCIENT_LIBRARY, WHISPERING_WELL, CARD_SHARP, ORACLE, RELIC_CURATOR, RIFT_GATE,
                FORGOTTEN_FORGE, ECHOING_ALTAR, MOONLIT_CARAVAN, STARVED_IDOL};
        regularEvents[services.nextInt(regularEvents.length)].resolve(services, hero, floor, difficulty);
    }
}

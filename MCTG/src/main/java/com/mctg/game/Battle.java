package com.mctg.game;

import com.mctg.cards.Card;
import com.mctg.cards.MonsterCard;
import com.mctg.cards.SpellCard;
import com.mctg.player.Player;
import java.util.List;
import java.util.ArrayList;

public class Battle {
    private final Player player1;
    private final Player player2;
    private final List<String> battleLog;
    private final Booster booster1;
    private final Booster booster2;

    public Battle(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.battleLog = new ArrayList<>();
        this.booster1 = Booster.getRandomBooster();
        this.booster2 = Booster.getRandomBooster();
    }

    public String start() {
        int rounds = 0;

        if (player1.getDeck().size() != 4 || player2.getDeck().size() != 4) {
            return "Error: Each player must have exactly 4 cards in their deck.";
        }

        List<Card> deck1 = new ArrayList<>(player1.getDeck());
        List<Card> deck2 = new ArrayList<>(player2.getDeck());

        battleLog.add("--- Battle Start: " + player1.getUsername() + " vs " + player2.getUsername() + " ---");
        battleLog.add(player1.getUsername() + " receives booster: " + booster1.getType());
        battleLog.add(player2.getUsername() + " receives booster: " + booster2.getType());

        while (!deck1.isEmpty() && !deck2.isEmpty() && rounds < 100) {
            rounds++;
            Card card1 = deck1.remove(0);
            Card card2 = deck2.remove(0);

            battleLog.add(String.format("Round %d: %s plays %s (%d damage) vs %s plays %s (%d damage)",
                    rounds, player1.getUsername(), card1.getName(), card1.getDamage(),
                    player2.getUsername(), card2.getName(), card2.getDamage()));

            int damage1 = calculateEffectiveDamage(card1, card2, booster1);
            int damage2 = calculateEffectiveDamage(card2, card1, booster2);

            battleLog.add(String.format("Calculated Damage: %s = %d, %s = %d",
                    player1.getUsername(), damage1,
                    player2.getUsername(), damage2));

            if (damage1 > damage2) {
                battleLog.add(player1.getUsername() + " wins this round. " + card2.getName() + " is removed from " + player2.getUsername() + "'s deck.");
                deck2.remove(card2);
            } else if (damage2 > damage1) {
                battleLog.add(player2.getUsername() + " wins this round. " + card1.getName() + " is removed from " + player1.getUsername() + "'s deck.");
                deck1.remove(card1);
            } else {
                battleLog.add("The round ends in a draw. No cards are removed.");
            }

            if (isBattleOver()) {
                return determineWinner(deck1, deck2);
            }
        }

        return determineWinner(deck1, deck2);
    }

    public int calculateEffectiveDamage(Card attacker, Card defender, Booster booster) {
        int baseDamage = attacker.calculateDamage(defender);

        // Handle special cases
        if (attacker instanceof MonsterCard && defender instanceof MonsterCard) {
            // Pure monster fights are not affected by element types
            return baseDamage;
        }

        // Apply elemental effectiveness for spell cards
        if (attacker instanceof SpellCard && defender instanceof MonsterCard) {
            switch (((SpellCard) attacker).getElement()) {
                case FIRE:
                    if (defender.getElement() == Card.ElementType.WATER) {
                        baseDamage *= 0.5;  // Fire vs Water is halved
                    }
                    break;
                case WATER:
                    if (defender.getElement() == Card.ElementType.FIRE) {
                        baseDamage *= 2;  // Water vs Fire is doubled
                    }
                    break;
                case NORMAL:
                    if (defender.getElement() == Card.ElementType.WATER) {
                        baseDamage *= 1.5;  // Normal vs Water is increased
                    }
                    break;
            }
        }

        // Apply booster effects
        switch (booster.getType()) {
            case DAMAGE_INCREASE:
                baseDamage *= 1.2;  // 20% increase
                break;
            case IMMUNITY:
                baseDamage = 0;  // Immune to damage this round
                break;
            case ELEMENT_NEUTRALIZER:
                baseDamage = attacker.getDamage();  // Neutralize elemental advantage
                break;
        }

        return baseDamage;
    }

    private String determineWinner(List<Card> deck1, List<Card> deck2) {
        if (deck1.isEmpty() && deck2.isEmpty()) {
            battleLog.add("The battle ends in a draw.");
            return "The battle ended in a draw.";
        } else if (deck1.isEmpty()) {
            battleLog.add(player2.getUsername() + " wins the battle!");
            updateElo(player2, player1);
            return player2.getUsername() + " wins the battle!";
        } else {
            battleLog.add(player1.getUsername() + " wins the battle!");
            updateElo(player1, player2);
            return player1.getUsername() + " wins the battle!";
        }
    }

    private void updateElo(Player winner, Player loser) {
        winner.increaseElo(3);
        loser.decreaseElo(5);
    }

    public List<String> getBattleLog() {
        return battleLog;
    }

    public boolean isBattleOver() {
        return player1.getDeck().isEmpty() || player2.getDeck().isEmpty();
    }
}

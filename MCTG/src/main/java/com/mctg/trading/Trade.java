package com.mctg.trading;

import com.mctg.cards.Card;
import com.mctg.player.Player;

public class Trade {
    private final String tradeId;
    private final Player requester;
    private final Card offeredCard;
    private final String requiredType;  // "monster" or "spell"
    private final Card.ElementType requiredElement;
    private final int minDamage;
    private boolean isCompleted;

    public Trade(String tradeId, Player requester, Card offeredCard, String requiredType,
                 Card.ElementType requiredElement, int minDamage) {
        this.tradeId = tradeId;
        this.requester = requester;
        this.offeredCard = offeredCard;
        this.requiredType = requiredType;
        this.requiredElement = requiredElement;
        this.minDamage = minDamage;
        this.isCompleted = false;

        // Lock the card for trade immediately
        requester.lockCard(offeredCard.getCardID());
    }

    public boolean tradeConditionMet(Card card) {
        if (minDamage > 0 && card.getDamage() < minDamage) {
            return false;
        }
        if (requiredType != null) {
            boolean isMonster = card instanceof com.mctg.cards.MonsterCard;
            boolean isSpell = card instanceof com.mctg.cards.SpellCard;

            if (requiredType.equalsIgnoreCase("monster") && !isMonster) {
                return false;
            }
            if (requiredType.equalsIgnoreCase("spell") && !isSpell) {
                return false;
            }
        }
        if (requiredElement != null && card.getElement() != requiredElement) {
            return false;
        }
        return true;
    }

    public void completeTrade(Player responder, Card responderCard) {
        // Unlock the offered card and the responder's card after the trade
        requester.unlockCard(offeredCard.getCardID());
        responder.unlockCard(responderCard.getCardID());
        isCompleted = true;
    }

    public void cancelTrade() {
        // Unlock the card if the trade is canceled
        requester.unlockCard(offeredCard.getCardID());
    }

    public String getTradeId() {
        return tradeId;
    }

    public Player getRequester() {
        return requester;
    }

    public Card getOfferedCard() {
        return offeredCard;
    }

    public String getRequiredType() { return requiredType; }

    public Card.ElementType getRequiredElement() { return requiredElement; }

    public int getMinDamage() { return minDamage; }

    public boolean isCompleted() {
        return isCompleted;
    }
}

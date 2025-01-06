package com.mctg.cards;

public class SpellCard extends Card {
    public SpellCard(String cardID, String name, int damage, ElementType element) {
        super(cardID, name, damage, element);
    }

    @Override
    public int calculateDamage(Card opponent) {
        int calculatedDamage = this.getDamage();

        // Kraken is immune against spells
        if (opponent.getName().contains("Kraken")) {
            return 0;
        }

        // Elemental effectiveness
        if (isEffectiveAgainst(opponent)) {
            calculatedDamage *= 2;
        } else if (isNotEffectiveAgainst(opponent)) {
            calculatedDamage /= 2;
        }

        return calculatedDamage;
    }

    private boolean isEffectiveAgainst(Card opponent) {
        return (this.getElement() == ElementType.FIRE && opponent.getElement() == ElementType.NORMAL) ||
                (this.getElement() == ElementType.WATER && opponent.getElement() == ElementType.FIRE) ||
                (this.getElement() == ElementType.NORMAL && opponent.getElement() == ElementType.WATER);
    }

    private boolean isNotEffectiveAgainst(Card opponent) {
        return (this.getElement() == ElementType.FIRE && opponent.getElement() == ElementType.WATER) ||
                (this.getElement() == ElementType.WATER && opponent.getElement() == ElementType.NORMAL) ||
                (this.getElement() == ElementType.NORMAL && opponent.getElement() == ElementType.FIRE);
    }
}

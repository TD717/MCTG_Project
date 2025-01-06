package com.mctg.cards;

import java.util.Objects;

public abstract class Card {
    private String cardID; // Unique identifier
    private String name;
    private int damage;
    private ElementType element;

    public Card(String cardID, String name, int damage, ElementType element) {
        this.cardID = java.util.UUID.randomUUID().toString(); // Generate unique ID
        this.name = name;
        this.damage = damage;
        this.element = element;
    }

    public String getCardID() {
        return cardID;
    }

    public String getName() {
        return name;
    }

    public int getDamage() {
        return damage;
    }

    public ElementType getElement() {
        return element;
    }

    public abstract int calculateDamage(Card opponent);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Card card = (Card) o;
        return Objects.equals(cardID, card.cardID); // Compare by unique ID
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardID);
    }

    public Object getId() {
        return cardID;
    }

    public enum ElementType {
        FIRE, WATER, NORMAL;
    }
}

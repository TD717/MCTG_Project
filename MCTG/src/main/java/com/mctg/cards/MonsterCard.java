package com.mctg.cards;

public class MonsterCard extends Card {
    public MonsterCard(String name, int damage, ElementType element) {
        super(name, damage, element);
    }

    @Override
    public int calculateDamage(Card opponent) {
        // Specialties
        if (this.getName().contains("Goblin") && opponent.getName().contains("Dragon")) {
            return 0; // Goblins are too afraid of Dragons to attack.
        }
        if (this.getName().contains("Wizard") && opponent.getName().contains("Ork")) {
            return this.getDamage() * 2; // Wizards control Orks.
        }
        if (this.getName().contains("Knight") && opponent instanceof SpellCard && opponent.getElement() == ElementType.WATER) {
            return 0; // Knights drown instantly when attacked by Water Spells.
        }
        if (this.getName().contains("Elf") && opponent.getName().contains("Dragon") && this.getElement() == ElementType.FIRE) {
            return this.getDamage() * 2; // FireElves evade Dragons' attacks.
        }

        // Pure monster fight, element has no effect
        return this.getDamage();
    }
}

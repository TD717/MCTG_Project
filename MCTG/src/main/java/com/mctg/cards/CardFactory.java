package com.mctg.cards;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardFactory {
    private static final Random random = new Random();

    public static List<Card> generateRandomCards(int count) {
        List<Card> cards = new ArrayList<>();
        String[] names = {"FireDragon", "WaterGoblin", "SpellKnight", "NormalElf", "FireWizard"};
        Card.ElementType[] elements = Card.ElementType.values();

        for (int i = 0; i < count; i++) {
            String name = names[random.nextInt(names.length)];
            int damage = 10 + random.nextInt(40);
            Card.ElementType element = elements[random.nextInt(elements.length)];
            if (name.contains("Spell")) {
                cards.add(new SpellCard(name, damage, element));
            } else {
                cards.add(new MonsterCard(name, damage, element));
            }
        }
        return cards;
    }
}

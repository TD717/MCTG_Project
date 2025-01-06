package test;

import com.mctg.cards.Card;
import com.mctg.cards.MonsterCard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;

public class CardTest {
    private Card card1;
    private Card card2;
    private Card card3;

    @BeforeEach
    public void setUp() {
        card1 = new MonsterCard(UUID.randomUUID().toString(), "Dragon", 100, Card.ElementType.FIRE);
        card2 = new MonsterCard(UUID.randomUUID().toString(), "Goblin", 50, Card.ElementType.NORMAL);
        card3 = new MonsterCard(UUID.randomUUID().toString(), "Dragon", 100, Card.ElementType.FIRE); // Different instance, same attributes
    }

    @Test
    public void testCardEquality() {
        assertNotEquals(card1, card3); // Different cardIDs, so they should not be equal
        assertEquals(card1.getName(), card3.getName());
        assertEquals(card1.getDamage(), card3.getDamage());
    }

    @Test
    public void testCardUUIDGeneration() {
        assertNotNull(card1.getCardID());
        assertNotNull(card2.getCardID());
        assertNotEquals(card1.getCardID(), card2.getCardID());
    }

    @Test
    public void testCardDamage() {
        assertEquals(100, card1.getDamage());
        assertEquals(50, card2.getDamage());
    }

    @Test
    public void testElementType() {
        assertEquals(Card.ElementType.FIRE, card1.getElement());
        assertEquals(Card.ElementType.NORMAL, card2.getElement());
    }

    @Test
    public void testCardHashCode() {
        assertEquals(card1.hashCode(), card1.hashCode());
        assertNotEquals(card1.hashCode(), card3.hashCode());
    }
}

package test;

import com.mctg.cards.Card;
import com.mctg.cards.MonsterCard;
import com.mctg.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {
    private Player player;
    private Card card1;
    private Card card2;

    @BeforeEach
    public void setUp() {
        player = new Player("Player1", "password123", playerId);
        card1 = new MonsterCard("Dragon", 100, Card.ElementType.FIRE);
        card2 = new MonsterCard("Goblin", 50, Card.ElementType.NORMAL);
    }

    @Test
    public void testPlayerInitialization() {
        assertEquals("Player1", player.getUsername());
        assertTrue(player.validatePassword("password123"));
        assertEquals(20, player.getCoins());
        assertEquals(100, player.getElo());
        assertEquals(0, player.getGamesPlayed());
    }

    @Test
    public void testAddCardToCardStack() {
        player.getCardStack().add(card1);
        assertTrue(player.getCardStack().contains(card1));
    }

    @Test
    public void testSetDeck() {
        List<Card> newDeck = List.of(card1, card2, card1, card2);
        player.setDeck(newDeck);
        assertEquals(4, player.getDeck().size());
        assertThrows(IllegalArgumentException.class, () -> player.setDeck(new ArrayList<>()));
    }

    @Test
    public void testBuyPackage() {
        List<Card> newCards = List.of(card1, card2);
        assertTrue(player.buyPackage(newCards));
        assertEquals(15, player.getCoins());
        assertTrue(player.getCardStack().containsAll(newCards));
    }

    @Test
    public void testEloAdjustment() {
        player.increaseElo(10);
        assertEquals(110, player.getElo());
        player.decreaseElo(15);
        assertEquals(95, player.getElo());
    }

    @Test
    public void testLockAndUnlockCard() {
        assertTrue(player.lockCard(card1.getCardID()));
        assertTrue(player.isCardLockedForTrade(card1.getCardID()));
        player.unlockCard(card1.getCardID());
        assertFalse(player.isCardLockedForTrade(card1.getCardID()));
    }
}

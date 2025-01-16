package test;

import com.mctg.cards.Card;
import com.mctg.cards.MonsterCard;
import com.mctg.game.Battle;
import com.mctg.game.Booster;
import com.mctg.player.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public class BattleTest {
    private Player player1;
    private Player player2;
    private Battle battle;

    private List<Card> deck1;
    private List<Card> deck2;

    @BeforeEach
    public void setUp() {
        player1 = new Player("Player1", "password123", UUID.randomUUID());
        player2 = new Player("Player2", "password123", UUID.randomUUID());

        deck1 = new ArrayList<>(List.of(
                new MonsterCard(UUID.randomUUID().toString(), "Dragon", 100, Card.ElementType.FIRE),
                new MonsterCard(UUID.randomUUID().toString(), "Goblin", 50, Card.ElementType.NORMAL),
                new MonsterCard(UUID.randomUUID().toString(), "Elf", 70, Card.ElementType.WATER),
                new MonsterCard(UUID.randomUUID().toString(), "Knight", 80, Card.ElementType.NORMAL)
        ));

        deck2 = new ArrayList<>(List.of(
                new MonsterCard(UUID.randomUUID().toString(), "Ork", 90, Card.ElementType.NORMAL),
                new MonsterCard(UUID.randomUUID().toString(), "Troll", 60, Card.ElementType.WATER),
                new MonsterCard(UUID.randomUUID().toString(), "Witch", 40, Card.ElementType.FIRE),
                new MonsterCard(UUID.randomUUID().toString(), "Golem", 50, Card.ElementType.NORMAL)
        ));

        assertEquals(4, deck1.size(), "Deck1 should have exactly 4 cards.");
        assertEquals(4, deck2.size(), "Deck2 should have exactly 4 cards.");

        player1.setDeck(deck1);
        player2.setDeck(deck2);

        battle = new Battle(player1, player2);
    }

    @Test
    public void testBattleInitialization() {
        assertNotNull(battle, "Battle should be initialized.");
        assertFalse(battle.isBattleOver(), "Battle should not be over at initialization.");
    }

    @Test
    public void testBattleOutcome() {
        Battle battle = new Battle(player1, player2);
        String result = battle.start();
        assertTrue(result.contains("wins the battle") || result.contains("draw"));
    }

    @Test
    public void testDeckSizeValidation() {
        List<Card> invalidDeck = new ArrayList<>(List.of(
                new MonsterCard(UUID.randomUUID().toString(), "Troll", 60, Card.ElementType.WATER),
                new MonsterCard(UUID.randomUUID().toString(), "Witch", 40, Card.ElementType.FIRE)
        ));

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> player1.setDeck(invalidDeck),
                "Expected IllegalArgumentException for invalid deck size."
        );

        assertTrue(thrown.getMessage().contains("Deck must contain exactly 4 cards."));
    }

    @Test
    public void testBattleLogRecordsBooster() {
        Battle battle = new Battle(player1, player2);
        battle.start();
        List<String> log = battle.getBattleLog();
        assertTrue(log.stream().anyMatch(entry -> entry.contains("receives booster")));
    }
}

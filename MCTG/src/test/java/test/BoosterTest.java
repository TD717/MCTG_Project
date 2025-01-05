package test;

import com.mctg.game.Booster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BoosterTest {

    private Booster damageIncreaseBooster;
    private Booster immunityBooster;
    private Booster neutralizerBooster;

    @BeforeEach
    public void setUp() {
        damageIncreaseBooster = new Booster(Booster.BoosterType.DAMAGE_INCREASE);
        immunityBooster = new Booster(Booster.BoosterType.IMMUNITY);
        neutralizerBooster = new Booster(Booster.BoosterType.ELEMENT_NEUTRALIZER);
    }

    @Test
    public void testBoosterInitialization() {
        assertNotNull(damageIncreaseBooster, "Booster should be initialized.");
        assertEquals(Booster.BoosterType.DAMAGE_INCREASE, damageIncreaseBooster.getType(),
                "Booster type should be DAMAGE_INCREASE.");

        assertNotNull(immunityBooster, "Immunity booster should be initialized.");
        assertEquals(Booster.BoosterType.IMMUNITY, immunityBooster.getType(),
                "Booster type should be IMMUNITY.");

        assertNotNull(neutralizerBooster, "Neutralizer booster should be initialized.");
        assertEquals(Booster.BoosterType.ELEMENT_NEUTRALIZER, neutralizerBooster.getType(),
                "Booster type should be ELEMENT_NEUTRALIZER.");
    }

    @Test
    public void testBoosterEquality() {
        Booster anotherDamageBooster = new Booster(Booster.BoosterType.DAMAGE_INCREASE);
        assertEquals(damageIncreaseBooster, anotherDamageBooster,
                "Boosters of the same type should be equal.");

        Booster differentBooster = new Booster(Booster.BoosterType.IMMUNITY);
        assertNotEquals(damageIncreaseBooster, differentBooster,
                "Boosters of different types should not be equal.");
    }

    @Test
    public void testBoosterHashCode() {
        Booster anotherNeutralizer = new Booster(Booster.BoosterType.ELEMENT_NEUTRALIZER);
        assertEquals(neutralizerBooster.hashCode(), anotherNeutralizer.hashCode(),
                "Boosters with the same type should have the same hash code.");
    }

    @Test
    public void testNullBooster() {
        Booster nullBooster = new Booster(null);
        assertNull(nullBooster.getType(),
                "Booster with null type should return null for type.");
    }

    @Test
    public void testDifferentBoosterInstances() {
        Booster booster1 = new Booster(Booster.BoosterType.IMMUNITY);
        Booster booster2 = new Booster(Booster.BoosterType.IMMUNITY);
        assertNotSame(booster1, booster2,
                "Each booster instance should be unique, even if they have the same type.");
    }
}

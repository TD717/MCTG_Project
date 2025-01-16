package com.mctg.game;

import java.util.Random;
import java.util.Objects;

public class Booster {
    public enum BoosterType {
        DAMAGE_INCREASE,
        IMMUNITY,
        ELEMENT_NEUTRALIZER
    }

    private final BoosterType type;

    public Booster(BoosterType type) {
        this.type = type;
    }

    public BoosterType getType() {
        return type;
    }

    public static Booster getRandomBooster() {
        BoosterType[] types = BoosterType.values();
        Random random = new Random();
        return new Booster(types[random.nextInt(types.length)]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booster booster = (Booster) o;
        return type == booster.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}

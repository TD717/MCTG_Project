package com.mctg.player;

import java.util.*;

public class UserRepository {
    private static UserRepository instance;
    private final Map<String, Player> players;

    private UserRepository() {
        this.players = new HashMap<>();
    }

    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public boolean exists(String username) {
        return players.containsKey(username);
    }

    public void save(Player player) {
        players.put(player.getUsername(), player);
    }

    public Optional<Player> find(String username) {
        return Optional.ofNullable(players.get(username));
    }

    public Collection<Player> getAllPlayers() {
        return players.values();
    }

}

package com.mctg.player;

import com.mctg.cards.Card;
import com.mctg.cards.MonsterCard;
import com.mctg.cards.SpellCard;
import com.mctg.db.DBConnection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;  // Import ResultSet
import java.util.*;

public class Player {
    private final String username;
    private String password;
    private int coins;
    private final List<Card> cardStack;
    private List<Card> deck;
    private List<String> lockedCards = new ArrayList<>();
    private int elo;
    private int gamesPlayed;
    private String activeToken;
    private final UUID playerId;

    public Player(String username, String password, UUID playerId) {
        this.username = username;
        this.setPassword(password);
        this.coins = 20;
        this.cardStack = new ArrayList<>();
        this.deck = new ArrayList<>();
        this.elo = 100;
        this.gamesPlayed = 0;
        this.activeToken = null;
        this.playerId = UUID.randomUUID();
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = hashPassword(password);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not available.");
        }
    }

    public boolean validatePassword(String inputPassword) {
        return this.password.equals(hashPassword(inputPassword));
    }

    public boolean validateToken(String token) {
        return UserService.getInstance().validateToken(token);
    }

    public int getCoins() {
        return coins;
    }

    public List<Card> getCardStack() {
        if (cardStack.isEmpty()) {
            loadCardStackFromDatabase();
        }
        return cardStack;
    }

    private void loadCardStackFromDatabase() {
        cardStack.clear();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM cards WHERE player_id = ?")) {

            pstmt.setObject(1, playerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Card card;
                String type = rs.getString("type");  // Fetch type from DB

                if ("MONSTER".equalsIgnoreCase(type)) {
                    card = new MonsterCard(
                            rs.getString("card_id"),
                            rs.getString("name"),
                            rs.getInt("damage"),
                            Card.ElementType.valueOf(rs.getString("element").toUpperCase())
                    );
                } else if ("SPELL".equalsIgnoreCase(type)) {
                    card = new SpellCard(
                            rs.getString("card_id"),
                            rs.getString("name"),
                            rs.getInt("damage"),
                            Card.ElementType.valueOf(rs.getString("element").toUpperCase())
                    );
                } else {
                    throw new IllegalArgumentException("Unknown card type: " + type);
                }

                cardStack.add(card);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> newDeck) {
        if (newDeck.size() != 4) {
            throw new IllegalArgumentException("Deck must contain exactly 4 cards.");
        }
        this.deck = newDeck;
    }

    public void incrementGamesPlayed() {
        gamesPlayed++;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getElo() {
        return elo;
    }

    public void increaseElo(int points) {
        elo += points;
    }

    public void decreaseElo(int points) {
        elo = Math.max(0, elo - points);
    }

    public boolean buyPackage(List<Card> newCards) {
        if (coins >= 5) {
            for (Card newCard : newCards) {
                if (!cardStack.contains(newCard)) {
                    cardStack.add(newCard);
                }
            }
            coins -= 5;
            return true;
        } else {
            throw new IllegalStateException("Not enough coins to buy a package.");
        }
    }

    public void setActiveToken(String token) {
        this.activeToken = token;
    }

    public String getActiveToken() {
        return activeToken;
    }

    public void logout() {
        this.activeToken = null;
    }

    public Card getCardById(String cardId) {
        for (Card card : cardStack) {
            if (card.getId().equals(cardId)) {
                return card;
            }
        }
        return null;
    }

    public boolean lockCard(String cardId) {
        if (!lockedCards.contains(cardId)) {
            lockedCards.add(cardId);
            return true;
        }
        return false;
    }

    public void unlockCard(String cardId) {
        lockedCards.remove(cardId);
    }

    public boolean isCardLockedForTrade(String cardId) {
        return lockedCards.contains(cardId);
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public UUID getPlayerId() { return playerId; }
}

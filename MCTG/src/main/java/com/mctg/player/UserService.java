package com.mctg.player;

import com.mctg.cards.Card;
import com.mctg.cards.MonsterCard;
import com.mctg.cards.SpellCard;
import com.mctg.db.DBConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;

public class UserService {
    private static UserService instance;
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 10 * 60 * 1000;  // 10 minutes
    private static final long TOKEN_EXPIRATION_MS = 60 * 60 * 1000;  // 1 hour

    private UserService() {}

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    // User Registration with SHA-256 Hashing
    public String register(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return "Missing or invalid username/password.";
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO players (username, password_hash) VALUES (?, ?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            pstmt.executeUpdate();
            return "Registration successful!";

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                return "Username already exists.";
            }
            e.printStackTrace();
            return "Database error during registration.";
        }
    }

    // Login with Rate Limiting and Token Generation
    public String login(String username, String password) {
        if (isLocked(username)) {
            return "Too many failed attempts. Please try again later.";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT password_hash, token FROM players WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password_hash");
                if (storedPassword.equals(hashPassword(password))) {
                    resetAttempts(username);
                    String token = rs.getString("token");

                    if (token == null || token.isEmpty()) {
                        token = generateToken(username);
                    }

                    return "{\"message\": \"Login successful!\", \"token\": \"" + token + "\"}";
                } else {
                    incrementAttempts(username);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "{\"message\": \"Invalid username or password.\"}";
    }

    // Generate Token with Expiry
    private String generateToken(String username) {
        String token = UUID.randomUUID().toString();
        long expiry = System.currentTimeMillis() + TOKEN_EXPIRATION_MS;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE players SET token = ?, token_expiry = ? WHERE username = ?")) {

            pstmt.setString(1, token);
            pstmt.setTimestamp(2, new Timestamp(expiry));
            pstmt.setString(3, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return token;
    }

    // Validate Token with Expiration Check
    public boolean validateToken(String token) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT token_expiry FROM players WHERE token = ?")) {

            pstmt.setString(1, token);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Timestamp expiry = rs.getTimestamp("token_expiry");
                return expiry != null && expiry.after(new Timestamp(System.currentTimeMillis()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Increment Failed Login Attempts
    private void incrementAttempts(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE players SET failed_attempts = failed_attempts + 1 WHERE username = ?")) {

            pstmt.setString(1, username);
            pstmt.executeUpdate();

            try (PreparedStatement checkAttempts = conn.prepareStatement(
                    "SELECT failed_attempts FROM players WHERE username = ?")) {
                checkAttempts.setString(1, username);
                ResultSet rs = checkAttempts.executeQuery();

                if (rs.next() && rs.getInt("failed_attempts") >= MAX_ATTEMPTS) {
                    lockUser(username);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Lock User by Setting Lockout Timestamp
    private void lockUser(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE players SET lockout_until = ? WHERE username = ?")) {

            long lockoutEnd = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
            pstmt.setTimestamp(1, new Timestamp(lockoutEnd));
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Reset Failed Attempts After Successful Login
    private void resetAttempts(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE players SET failed_attempts = 0, lockout_until = NULL WHERE username = ?")) {

            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Check if User is Locked
    private boolean isLocked(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT lockout_until FROM players WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Timestamp lockout = rs.getTimestamp("lockout_until");
                return lockout != null && lockout.after(new Timestamp(System.currentTimeMillis()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // SHA-256 Password Hashing
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password.", e);
        }
    }

    // Get Player by Username
    public Player getPlayer(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM players WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));

                Player player = new Player(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        playerId
                );
                player.setElo(rs.getInt("elo"));
                player.setCoins(rs.getInt("coins"));
                player.setGamesPlayed(rs.getInt("games_played"));

                // Force reload the card stack for the specific player
                player.loadCardStackFromDatabase();

                return player;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public UUID getPlayerIdFromDatabase(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT player_id FROM players WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return UUID.fromString(rs.getString("player_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUserProfile(String username) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT username, elo, games_played FROM players WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return String.format("User: %s\nELO: %d\nGames Played: %d",
                        rs.getString("username"), rs.getInt("elo"), rs.getInt("games_played"));
            } else {
                return "404 Not Found - User does not exist.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "500 Internal Server Error - Database error.";
        }
    }

    public String updateUserProfile(String username, Map<String, String> updates) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBConnection.getConnection();
            StringBuilder sql = new StringBuilder("UPDATE players SET ");

            boolean updateRequired = false;
            if (updates.containsKey("username")) {
                sql.append("username = ?, ");
                updateRequired = true;
            }
            if (updates.containsKey("password")) {
                sql.append("password_hash = ?, ");
                updateRequired = true;
            }

            if (!updateRequired) {
                return "400 Bad Request - No fields to update.";
            }

            // Remove trailing comma
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE username = ?");

            pstmt = conn.prepareStatement(sql.toString());

            int index = 1;
            if (updates.containsKey("username")) {
                pstmt.setString(index++, updates.get("username"));
            }
            if (updates.containsKey("password")) {
                pstmt.setString(index++, hashPassword(updates.get("password")));
            }
            pstmt.setString(index, username);

            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                return "Profile updated successfully.";
            } else {
                return "404 Not Found - User does not exist.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "500 Internal Server Error - Database error.";
        } finally {
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Get Player's Deck by Username
    public List<Card> getDeck(String username) {
        List<Card> deck = new ArrayList<>();

        String sql = "SELECT c.card_id, c.name, c.damage, c.element, c.type " +
                "FROM decks d " +
                "JOIN cards c ON d.card_id = c.card_id " +
                "WHERE d.player_username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String cardId = rs.getString("card_id");
                String name = rs.getString("name");
                int damage = rs.getInt("damage");
                Card.ElementType element = Card.ElementType.valueOf(rs.getString("element"));
                String type = rs.getString("type");

                Card card = createCardInstance(cardId, name, damage, element, type);
                deck.add(card);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Database error while fetching deck.");
        }

        return deck;
    }

    // Helper method to create Card instances
    private Card createCardInstance(String cardId, String name, int damage, Card.ElementType element, String type) {
        if (type.equalsIgnoreCase("monster")) {
            return new MonsterCard(cardId, name, damage, element);  // Pass cardId
        } else if (type.equalsIgnoreCase("spell")) {
            return new SpellCard(cardId, name, damage, element);  // Pass cardId
        } else {
            throw new IllegalArgumentException("Unknown card type: " + type);
        }
    }


    // Update Player's Deck by Username
    public String updateDeck(String username, List<String> cardIds) {
        Player player = getPlayer(username);
        if (player == null) {
            return "Player not found.";
        }

        // Early check to ensure exactly 4 card IDs are provided
        if (cardIds.size() != 4) {
            return "You must provide exactly 4 card IDs to update the deck.";
        }

        List<Card> newDeck = new ArrayList<>();
        List<String> invalidCards = new ArrayList<>();

        // Validate each card ID and add to the new deck
        for (String cardId : cardIds) {
            Card card = player.getCardById(cardId);
            if (card == null) {
                invalidCards.add(cardId);
            }
            if (newDeck.contains(card)) {
                return "Duplicate cards are not allowed.";
            }
            newDeck.add(card);
        }

        if (!invalidCards.isEmpty()) {
            return "Invalid card IDs: " + String.join(", ", invalidCards);
        }

        // Ensure the deck contains exactly 4 unique cards
        if (newDeck.size() != 4) {
            return "Deck must contain exactly 4 unique cards.";
        }

        // Update deck in the database with transaction handling
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement deleteStmt = conn.prepareStatement(
                     "DELETE FROM decks WHERE player_username = ?");
             PreparedStatement insertStmt = conn.prepareStatement(
                     "INSERT INTO decks (player_username, card_id) VALUES (?, ?)")) {

            conn.setAutoCommit(false);  // Start transaction

            // Clear the old deck
            deleteStmt.setString(1, username);
            deleteStmt.executeUpdate();

            // Insert new deck
            for (Card card : newDeck) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, card.getCardID());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();

            conn.commit();
            player.setDeck(newDeck);

            return "Deck updated successfully.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error while updating deck.";
        }
    }

    // Leaderboard
    public List<Player> getLeaderboard() {
        List<Player> players = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT * FROM players ORDER BY elo DESC, games_played DESC")) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Extract the player_id from the ResultSet
                UUID playerId = UUID.fromString(rs.getString("player_id"));

                Player player = new Player(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        playerId
                );
                player.setElo(rs.getInt("elo"));
                player.setCoins(rs.getInt("coins"));
                player.setGamesPlayed(rs.getInt("games_played"));
                players.add(player);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return players;
    }

    public String getUsernameFromToken(String token) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT username FROM players WHERE token = ?")) {

            pstmt.setString(1, token);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Token " + token + " maps to user: " + rs.getString("username"));
                return rs.getString("username");
            } else {
                System.out.println("Token not found in DB.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}



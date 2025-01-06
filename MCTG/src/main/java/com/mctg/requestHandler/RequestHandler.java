package com.mctg.requestHandler;

import com.mctg.cards.Card;
import com.mctg.db.DBConnection;
import com.mctg.player.UserService;
import com.mctg.trading.TradeController;
import com.mctg.game.Battle;
import com.mctg.player.Player;
import com.mctg.server.HttpMethod;

import java.util.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;


public class RequestHandler {
    private final UserService userService;
    private final TradeController tradeController;

    public RequestHandler(UserService userService, TradeController tradeController) {
        this.userService = userService;
        this.tradeController = tradeController;
    }

    // Handle incoming HTTP requests
    public String handleRequest(Request request) {
        HttpMethod method = request.getMethod();
        String path = request.getPath();
        Map<String, String> body = request.getBody();

        if (requiresTokenValidation(path)) {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return "401 Unauthorized - Missing or invalid token format.";
            }

            String token = authHeader.substring(7);  // Extract token after "Bearer "
            System.out.println("Extracted Token: " + token);  // Debugging line

            if (token.isEmpty() || !userService.validateToken(token)) {
                System.out.println("Token validation failed.");
                return "401 Unauthorized - Invalid or missing token.";
            }
        }

        switch (path) {
            case "/register":
                return handleRegister(method, body);

            case "/login":
                return handleLogin(method, body);

            case "/users":
                return handleUserRequest(method, body);

            case "/cards":
                if (method == HttpMethod.GET) {
                    return handleCardsRequest(body, request);
                }
                break;

            case "/deck":
                if (method == HttpMethod.PUT) {
                    return handleDeckRequest(request, body);
                }
                break;

            case "/trade":
                return handleTradeRequest(request, body);

            case "/trade/accept":
                if (method == HttpMethod.POST) {
                    return tradeController.acceptTrade(
                            body.get("tradeId"),
                            body.get("username")
                    );
                }
                break;

            case "/trade/delete":
                if (method == HttpMethod.POST) {
                    return tradeController.cancelTrade(
                            body.get("tradeId"),
                            body.get("username")
                    );
                }
                break;

            case "/battle":
                if (method == HttpMethod.POST) {
                    return handleBattle(body);
                }
                break;

            case "/package":
                if (method == HttpMethod.POST) {
                    return handlePackagePurchase(body, request);
                }
                break;

            case "/scoreboard":
                if (method == HttpMethod.GET) {
                    return handleScoreboard();
                }
                break;

            default:
                return "404 Not Found";
        }
        return "Invalid method.";
    }

    // Helper method to bypass token validation for specific paths
    private boolean requiresTokenValidation(String path) {
        return !path.equals("/register") && !path.equals("/login");
    }

    private String handleRegister(HttpMethod method, Map<String, String> body) {
        if (method != HttpMethod.POST) {
            return "405 Method Not Allowed";
        }

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return "Missing or invalid username/password.";
        }

        return userService.register(username, password);
    }

    private String handleLogin(HttpMethod method, Map<String, String> body) {
        if (method != HttpMethod.POST) {
            return "405 Method Not Allowed";
        }

        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return "Missing or invalid username/password.";
        }

        return userService.login(username, password);
    }

    private String handleUserRequest(HttpMethod method, Map<String, String> body) {
        String username = body.get("username");
        String token = body.get("token");

        if (username == null || token == null || username.isEmpty() || token.isEmpty()) {
            return "400 Bad Request - Missing username or token.";
        }

        if (method == HttpMethod.GET) {
            return userService.getUserProfile(username);
        } else if (method == HttpMethod.PUT) {
            String newUsername = body.get("new_username");
            String newPassword = body.get("password");

            // Collect updates for username and password only
            Map<String, String> updates = new HashMap<>();
            if (newUsername != null && !newUsername.isEmpty()) {
                updates.put("username", newUsername);
            }
            if (newPassword != null && !newPassword.isEmpty()) {
                updates.put("password", newPassword);
            }

            if (updates.isEmpty()) {
                return "400 Bad Request - No fields to update.";
            }

            return userService.updateUserProfile(username, updates);
        }

        return "405 Method Not Allowed";
    }

    private String handlePackagePurchase(Map<String, String> body, Request request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "401 Unauthorized - Missing or invalid token.";
        }

        String token = authHeader.substring(7);
        String username = userService.getUsernameFromToken(token);

        if (username == null) {
            return "401 Unauthorized - Invalid token.";
        }

        Player player = userService.getPlayer(username);
        if (player == null) {
            return "404 Not Found - Player does not exist.";
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Deduct coins for package purchase
            PreparedStatement coinStmt = conn.prepareStatement(
                    "UPDATE players SET coins = coins - 5 WHERE username = ? AND coins >= 5"
            );
            coinStmt.setString(1, username);
            int rowsUpdated = coinStmt.executeUpdate();

            if (rowsUpdated == 0) {
                System.out.println("Package purchase failed: Not enough coins for " + username);
                return "400 Bad Request - Not enough coins.";
            }

            // Fetch the correct player_id from the database
            UUID playerId = userService.getPlayerIdFromDatabase(username);
            if (playerId == null) {
                System.out.println("Failed to retrieve player_id for " + username);
                return "500 Internal Server Error - Player ID not found.";
            }

            // Atomic card assignment (UPDATE and RETURNING)
            PreparedStatement assignCards = conn.prepareStatement(
                    "UPDATE cards " +
                            "SET player_id = ? " +
                            "WHERE card_id IN (SELECT card_id FROM cards WHERE player_id IS NULL ORDER BY RANDOM() LIMIT 5) " +
                            "RETURNING card_id"
            );
            assignCards.setObject(1, playerId);
            ResultSet rs = assignCards.executeQuery();

            int cardCount = 0;
            PreparedStatement addToPlayerCardsStmt = conn.prepareStatement(
                    "INSERT INTO player_cards (username, card_id) VALUES (?, ?::uuid) ON CONFLICT DO NOTHING"
            );

            while (rs.next()) {
                addToPlayerCardsStmt.setString(1, username);
                addToPlayerCardsStmt.setObject(2, rs.getObject("card_id"));
                addToPlayerCardsStmt.addBatch();
                cardCount++;
            }

            // Rollback if less than 5 cards are available
            if (cardCount < 5) {
                conn.rollback();
                System.out.println("Package purchase failed: Not enough available cards.");
                return "500 Internal Server Error - Not enough available cards.";
            }

            // Execute batch to add to player_cards table
            addToPlayerCardsStmt.executeBatch();
            conn.commit();

            System.out.println("Cards successfully assigned to " + username);

            // Force reload player's card stack after purchase
            player.getCardStack().clear();
            player.getCardStack();
            System.out.println("Cards reloaded for " + username + ": " + player.getCardStack().size());

            return "Package purchased successfully.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "500 Internal Server Error - Database issue: " + e.getMessage();
        }
    }

    private String handleCardsRequest(Map<String, String> body, Request request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "401 Unauthorized - Missing or invalid token format.";
        }

        String token = authHeader.substring(7);  // Extract token
        String username = userService.getUsernameFromToken(token);

        if (username == null) {
            return "401 Unauthorized - Invalid token.";
        }

        Player player = userService.getPlayer(username);
        if (player == null) {
            return "404 Not Found - Player not found.";
        }

        // Force reload the card stack
        player.getCardStack().clear();
        player.getCardStack();

        List<Card> cardStack = player.getCardStack();

        if (cardStack.isEmpty()) {
            return "{\"message\": \"No cards available\"}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{ \"cards\": [");

        for (int i = 0; i < cardStack.size(); i++) {
            json.append("\"").append(cardStack.get(i).getCardID()).append("\"");
            if (i < cardStack.size() - 1) {
                json.append(", ");
            }
        }
        json.append("] }");

        return json.toString();
    }

    private String handleDeckRequest(Request request, Map<String, String> body) {
        String username = body.get("username");
        String token = request.getHeader("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            return "401 Unauthorized - Missing or invalid token.";
        }

        // Extract the token without the "Bearer " prefix
        token = token.substring(7);

        // Check if the username is provided
        if (username == null || username.isEmpty()) {
            return "Error: Missing username.";
        }

        // Validate the token against the username
        String validatedUsername = userService.getUsernameFromToken(token);
        if (validatedUsername == null || !validatedUsername.equals(username)) {
            return "401 Unauthorized - Invalid token.";
        }

        // Fetch the player details using the username
        Player player = userService.getPlayer(username);
        if (player == null) {
            return "404 Not Found - Player does not exist.";
        }

        if (request.getMethod().equals(HttpMethod.GET)) {
            // Fetch the deck for the player
            List<Card> deck = userService.getDeck(username);
            if (deck.isEmpty()) {
                return "The deck is empty.";
            }
            StringBuilder deckDetails = new StringBuilder("Current Deck:\n");
            for (Card card : deck) {
                deckDetails.append(card.getName())
                        .append(" (")
                        .append(card.getDamage())
                        .append(" damage)\n");
            }
            return deckDetails.toString();
        }

        if (request.getMethod().equals(HttpMethod.PUT)) {
            String cardIdsString = body.get("cardIds");

            if (cardIdsString != null && !cardIdsString.isEmpty()) {
                List<String> cardIds = parseCardIds(cardIdsString);

                // Ensure we are getting exactly 4 card IDs
                if (cardIds.size() == 4) {
                    return userService.updateDeck(username, cardIds);
                }
                return "400 Bad Request - You must provide exactly 4 card IDs to update the deck.";
            }
            return "400 Bad Request - Missing cardIds.";
        }

        return "Invalid method for deck management.";
    }

    private String handleTradeRequest(Request request, Map<String, String> body) {
        // Handling POST request for trade creation
        if (request.getMethod().equals(HttpMethod.POST)) {
            // Validate that all required fields are present in the body
            String username = body.get("username");
            String cardId = body.get("cardId");
            String requiredType = body.get("requiredType");
            String element = body.get("element");

            if (username == null || cardId == null || requiredType == null || element == null) {
                return "400 Bad Request - Missing required fields.";
            }

            // Parse minDamage with a default of 0 if not provided
            int minDamage = Integer.parseInt(body.getOrDefault("minDamage", "0"));

            // Call the tradeController to create the trade
            return tradeController.createTrade(
                    username,
                    cardId,
                    requiredType,
                    element,
                    minDamage
            );
        }

        // Handling GET request for listing trades
        else if (request.getMethod().equals(HttpMethod.GET)) {
            return tradeController.listTrades();
        }

        // Return error message for unsupported methods
        return "Invalid method for trading.";
    }

    private String handleBattle(Map<String, String> body) {
        String player1Username = body.get("player1");
        String player2Username = body.get("player2");

        if (player1Username == null || player2Username == null || player1Username.isEmpty() || player2Username.isEmpty()) {
            return "Error: Missing or invalid player usernames.";
        }

        Player player1 = userService.getPlayer(player1Username);
        Player player2 = userService.getPlayer(player2Username);

        if (player1 == null) {
            return "Error: Player 1 (" + player1Username + ") not found.";
        }
        if (player2 == null) {
            return "Error: Player 2 (" + player2Username + ") not found.";
        }

        Battle battle = new Battle(player1, player2);
        String result = battle.start();

        player1.incrementGamesPlayed();
        player2.incrementGamesPlayed();

        return result + "\nBattle Log:\n" + String.join("\n", battle.getBattleLog());
    }

    private void rollbackTransaction() {
        try (Connection conn = DBConnection.getConnection()) {
            conn.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private UUID getPlayerId(String username, Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
                "SELECT player_id FROM players WHERE username = ?");
        pstmt.setString(1, username);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return UUID.fromString(rs.getString("player_id"));
        } else {
            throw new SQLException("Player not found.");
        }
    }

    private String handleScoreboard() {
        return Battle.generateScoreboard();
    }

    private List<String> parseCardIds(String cardIdsString) {
        String cleanCardIdsString = cardIdsString.replaceAll("[\\[\\]\"]", "").trim();
        List<String> cardIds = Arrays.asList(cleanCardIdsString.split(","));
        System.out.println("Parsed card IDs: " + cardIds);
        return cardIds;
    }

}

package com.mctg.trading;

import com.mctg.player.Player;
import com.mctg.player.UserService;
import com.mctg.db.DBConnection;

import java.sql.*;
import java.util.UUID;

public class TradeController {
    private final UserService userService;

    public TradeController() {
        this.userService = UserService.getInstance();
    }

    public TradeController(UserService userService) {
        this.userService = userService;
    }

    // Create a new trade
    public String createTrade(String username, String cardId, String requiredType, String element, int minDamage) {
        Player player = userService.getPlayer(username);
        if (player == null) {
            return "Trade creation failed: Player not found.";
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Verify card ownership and lock status directly in the database
            PreparedStatement verifyCardStmt = conn.prepareStatement(
                    "SELECT card_id FROM cards WHERE card_id = ? AND player_id = ? AND locked = FALSE"
            );
            verifyCardStmt.setObject(1, UUID.fromString(cardId));
            verifyCardStmt.setObject(2, player.getPlayerId());

            try (ResultSet rs = verifyCardStmt.executeQuery()) {
                if (!rs.next()) {
                    return "Trade creation failed: Card not found or already locked.";
                }
            }

            // Lock the card to prevent concurrent trades
            PreparedStatement lockCardStmt = conn.prepareStatement(
                    "UPDATE cards SET locked = TRUE WHERE card_id = ? AND player_id = ? AND locked = FALSE"
            );
            lockCardStmt.setObject(1, UUID.fromString(cardId));
            lockCardStmt.setObject(2, player.getPlayerId());
            int rowsUpdated = lockCardStmt.executeUpdate();

            if (rowsUpdated == 0) {
                return "Trade creation failed: Card is already locked or does not belong to the player.";
            }

            // Insert trade entry into the database
            String tradeId = UUID.randomUUID().toString();
            PreparedStatement insertTrade = conn.prepareStatement(
                    "INSERT INTO trades (trade_id, requester_username, card_id, required_type, required_element, min_damage) " +
                            "VALUES (?, ?, ?, ?, ?, ?)"
            );
            insertTrade.setObject(1, UUID.fromString(tradeId));
            insertTrade.setString(2, username);
            insertTrade.setObject(3, UUID.fromString(cardId));
            insertTrade.setString(4, requiredType);
            insertTrade.setString(5, element);
            insertTrade.setInt(6, minDamage);
            insertTrade.executeUpdate();

            conn.commit();
            return "{\"message\": \"Trade created successfully with ID: " + tradeId + "\"}";

        } catch (SQLException e) {
            e.printStackTrace();
            return "500 Internal Server Error - Database issue: " + e.getMessage();
        }
    }

    // Accept a trade
    public String acceptTrade(String tradeId, String accepterUsername) {
        Player acceptingPlayer = userService.getPlayer(accepterUsername);
        if (acceptingPlayer == null) {
            return "Trade acceptance failed: Player not found.";
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Verify trade existence and fetch associated card
            PreparedStatement getTradeStmt = conn.prepareStatement(
                    "SELECT card_id, requester_username FROM trades WHERE trade_id = ? AND is_completed = FALSE"
            );
            getTradeStmt.setObject(1, UUID.fromString(tradeId));
            ResultSet tradeRs = getTradeStmt.executeQuery();

            if (!tradeRs.next()) {
                return "Trade not found.";
            }

            UUID cardId = (UUID) tradeRs.getObject("card_id");

            // Transfer the card to the accepting player
            PreparedStatement transferCardStmt = conn.prepareStatement(
                    "UPDATE cards SET player_id = ?, locked = FALSE WHERE card_id = ?"
            );
            transferCardStmt.setObject(1, acceptingPlayer.getPlayerId());
            transferCardStmt.setObject(2, cardId);
            transferCardStmt.executeUpdate();

            // Mark the trade as completed
            PreparedStatement completeTradeStmt = conn.prepareStatement(
                    "UPDATE trades SET is_completed = TRUE WHERE trade_id = ?"
            );
            completeTradeStmt.setObject(1, UUID.fromString(tradeId));
            completeTradeStmt.executeUpdate();

            conn.commit();
            return "Trade accepted successfully.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "500 Internal Server Error - Database issue: " + e.getMessage();
        }
    }

    // Cancel a trade
    public String cancelTrade(String tradeId, String username) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Verify trade existence and ownership
            PreparedStatement getTradeStmt = conn.prepareStatement(
                    "SELECT card_id FROM trades WHERE trade_id = ? AND requester_username = ? AND is_completed = FALSE"
            );
            getTradeStmt.setObject(1, UUID.fromString(tradeId));
            getTradeStmt.setString(2, username);
            ResultSet rs = getTradeStmt.executeQuery();

            if (!rs.next()) {
                return "Trade cancellation failed: Trade not found.";
            }

            UUID cardId = (UUID) rs.getObject("card_id");

            // Delete the trade
            PreparedStatement deleteTradeStmt = conn.prepareStatement(
                    "DELETE FROM trades WHERE trade_id = ?"
            );
            deleteTradeStmt.setObject(1, UUID.fromString(tradeId));
            deleteTradeStmt.executeUpdate();

            // Unlock the card
            PreparedStatement unlockCardStmt = conn.prepareStatement(
                    "UPDATE cards SET locked = FALSE WHERE card_id = ?"
            );
            unlockCardStmt.setObject(1, cardId);
            unlockCardStmt.executeUpdate();

            conn.commit();
            return "Trade cancelled successfully.";

        } catch (SQLException e) {
            e.printStackTrace();
            return "500 Internal Server Error - Database issue: " + e.getMessage();
        }
    }

    // List trades
    public String listTrades() {
        StringBuilder result = new StringBuilder();
        result.append("{ \"message\": \"Active Trades\", \"trades\": [");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT trade_id, requester_username, required_type, required_element, min_damage " +
                             "FROM trades WHERE is_completed = FALSE"
             );
             ResultSet rs = stmt.executeQuery()) {

            boolean firstTrade = true;
            while (rs.next()) {
                if (!firstTrade) {
                    result.append(",");
                }
                firstTrade = false;

                result.append("{");
                result.append("\"tradeId\": \"").append(rs.getString("trade_id")).append("\", ");
                result.append("\"requester\": \"").append(rs.getString("requester_username")).append("\", ");
                result.append("\"requiredType\": \"").append(rs.getString("required_type")).append("\", ");
                result.append("\"requiredElement\": \"").append(rs.getString("required_element")).append("\", ");
                result.append("\"minDamage\": ").append(rs.getInt("min_damage"));
                result.append("}");
            }

            result.append("]}");

        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"message\": \"500 Internal Server Error - Database issue\", \"error\": \"" + e.getMessage() + "\"}";
        }

        return result.toString();
    }
}

package com.mctg.trading;

import com.mctg.db.DBConnection;

import java.util.HashMap;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class TradeRepository {
    private static TradeRepository instance;
    private final Map<String, Trade> trades;

    private TradeRepository() {
        this.trades = new HashMap<>();
    }

    public static synchronized TradeRepository getInstance() {
        if (instance == null) {
            instance = new TradeRepository();
        }
        return instance;
    }

    public synchronized void addTrade(Trade trade) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO trades (trade_id, requester_username, card_id, required_type, required_element, min_damage, is_completed) " +
                             "VALUES (?, ?, ?, ?, ?, ?, FALSE)")) {

            pstmt.setObject(1, UUID.fromString(trade.getTradeId()));
            pstmt.setString(2, trade.getRequester().getUsername());
            pstmt.setObject(3, UUID.fromString(trade.getOfferedCard().getCardID()));
            pstmt.setString(4, trade.getRequiredType());
            pstmt.setString(5, trade.getRequiredElement() != null ? trade.getRequiredElement().toString() : null);
            pstmt.setInt(6, trade.getMinDamage());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to persist trade.");
        }
    }

    // Remove trade and unlock card in DB
    public synchronized void removeTrade(String tradeId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM trades WHERE trade_id = ?")) {

            pstmt.setObject(1, UUID.fromString(tradeId));
            pstmt.executeUpdate();

            Trade trade = trades.remove(tradeId);
            if (trade != null) {
                PreparedStatement unlockStmt = conn.prepareStatement(
                        "UPDATE cards SET locked = FALSE WHERE card_id = ?");
                unlockStmt.setObject(1, UUID.fromString(trade.getOfferedCard().getCardID()));
                unlockStmt.executeUpdate();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to remove trade.");
        }
    }


    public synchronized Trade getTrade(String tradeId) {
        return trades.get(tradeId);
    }

    public synchronized Map<String, Trade> getAllTrades() {
        return new HashMap<>(trades);
    }

    public synchronized void completeTrade(String tradeId, Trade trade, String responderUsername, String responderCardId) {
        if (trade != null) {
            trade.completeTrade(trade.getRequester(), trade.getOfferedCard());
            trades.remove(tradeId);
        }
    }
}

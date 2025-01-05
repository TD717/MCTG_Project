package com.mctg.trading;

import com.mctg.cards.Card;
import com.mctg.cards.Card.ElementType;
import com.mctg.db.DBConnection;
import com.mctg.player.Player;
import com.mctg.player.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class TradeController {
    // Correctly initialize userService and tradeRepository
    private final TradeRepository tradeRepository = TradeRepository.getInstance();
    private final UserService userService = UserService.getInstance();

    public String createTrade(String username, String cardId, String requiredType, String element, int minDamage) {
        Player player = userService.getPlayer(username);
        if (player == null) {
            return "Trade creation failed: Player not found.";
        }

        Card offeredCard = player.getCardById(cardId);
        if (offeredCard == null) {
            return "Trade creation failed: Card not found.";
        }

        // Check if the card is already in the active deck
        if (player.getDeck().contains(offeredCard)) {
            return "Trade creation failed: Cannot trade a card from the active deck.";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE cards SET locked = TRUE WHERE card_id = ? AND player_id = ? AND locked = FALSE")) {

            pstmt.setObject(1, UUID.fromString(cardId));
            pstmt.setObject(2, player.getPlayerId());
            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated == 0) {
                return "Trade creation failed: Card is already locked or does not belong to the player.";
            }

            // Create trade entry in DB
            String tradeId = UUID.randomUUID().toString();
            PreparedStatement insertTrade = conn.prepareStatement(
                    "INSERT INTO trades (trade_id, requester_username, card_id, required_type, required_element, min_damage) " +
                            "VALUES (?, ?, ?, ?, ?, ?)");

            insertTrade.setObject(1, UUID.fromString(tradeId));
            insertTrade.setString(2, username);
            insertTrade.setObject(3, UUID.fromString(cardId));
            insertTrade.setString(4, requiredType);
            insertTrade.setString(5, element);
            insertTrade.setInt(6, minDamage);
            insertTrade.executeUpdate();

            return "Trade created successfully with ID: " + tradeId;

        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error during trade creation.";
        }
    }


    public String acceptTrade(String username, String tradeId, String cardId) {
        Trade trade = tradeRepository.getTrade(tradeId);
        if (trade == null) {
            return "Trade not found.";
        }

        Player responder = userService.getPlayer(username);
        if (responder == null) {
            return "Trade failed: Responder not found.";
        }

        Card responderCard = responder.getCardById(cardId);
        if (responderCard == null || responder.getDeck().contains(responderCard)) {
            return "Trade failed: Card not found or is part of the active deck.";
        }

        if (!trade.tradeConditionMet(responderCard)) {
            return "Trade failed: Card does not meet trade conditions.";
        }

        Player requester = trade.getRequester();

        // Unlock both traded cards
        requester.unlockCard(trade.getOfferedCard().getCardID());
        responder.unlockCard(responderCard.getCardID());

        requester.getCardStack().remove(trade.getOfferedCard());
        responder.getCardStack().remove(responderCard);

        requester.getCardStack().add(responderCard);
        responder.getCardStack().add(trade.getOfferedCard());

        tradeRepository.removeTrade(tradeId);
        return "Trade successful!";
    }

    public String deleteTrade(String username, String tradeId) {
        Trade trade = tradeRepository.getTrade(tradeId);
        if (trade == null || !trade.getRequester().getUsername().equals(username)) {
            return "Trade deletion failed: Trade not found or unauthorized.";
        }

        // Unlock card if trade is deleted
        trade.cancelTrade();
        tradeRepository.removeTrade(tradeId);
        return "Trade deleted successfully.";
    }

    public String listTrades() {
        StringBuilder result = new StringBuilder("Active Trades:\n");
        for (Trade trade : tradeRepository.getAllTrades().values()) {
            result.append(trade.toString()).append("\n");
        }
        return result.toString();
    }
}

package com.mctg.player;

import com.mctg.cards.Card;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class UserController {
    private static UserController instance;
    private final UserService userService;

    private UserController() {
        this.userService = UserService.getInstance();
    }

    public static UserController getInstance() {
        if (instance == null) {
            instance = new UserController();
        }
        return instance;
    }

    public String register(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return "Missing or invalid username/password.";
        }
        return userService.register(username, password);
    }

    public String login(Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return "Missing or invalid username/password.";
        }
        return userService.login(username, password);
    }

    public String updateDeck(String username, List<String> cardIds, String token) {
        Player player = userService.getPlayer(username);

        if (player == null) {
            return "Unauthorized request: Player not found.";
        }

        if (!player.validateToken(token)) {
            return "Unauthorized request: Invalid token.";
        }

        // If cardIds is null or empty, randomly select 4 cards
        if (cardIds == null || cardIds.isEmpty()) {
            List<String> allCardIds = userService.getAllCardIdsForPlayer(username);

            if (allCardIds.size() < 4) {
                return "400 Bad Request - Player does not have enough cards to form a deck.";
            }

            // Randomly select 4 cards
            Collections.shuffle(allCardIds);
            cardIds = allCardIds.subList(0, 4);
        }

        // Pass the validated or randomly selected cardIds to the service
        return userService.updateDeck(username, cardIds);
    }
}

package com.mctg.player;

import com.mctg.cards.Card;
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
        if (player == null || !player.validateToken(token)) {
            return "Unauthorized request.";
        }
        if (cardIds.size() != 4) {
            return "Deck must contain exactly 4 cards.";
        }
        return userService.updateDeck(username, cardIds);
    }
}

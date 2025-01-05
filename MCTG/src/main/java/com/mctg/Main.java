package com.mctg;

import com.mctg.player.UserService;
import com.mctg.requestHandler.RequestHandler;
import com.mctg.server.MTCGServer;
import com.mctg.trading.TradeController;

public class Main {
    public static void main(String[] args) {
        int port = 10001; // Default port

        // Initialize dependencies
        UserService userService = UserService.getInstance();
        TradeController tradeController = new TradeController();
        RequestHandler requestHandler = new RequestHandler(userService, tradeController);

        // Log server startup
        System.out.println("Starting server on port: " + port);

        // Start the server
        MTCGServer server = new MTCGServer(port, requestHandler);
        server.start();
    }
}


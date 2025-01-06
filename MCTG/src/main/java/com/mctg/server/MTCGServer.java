package com.mctg.server;

import com.mctg.db.DBConnection;
import com.mctg.requestHandler.Request;
import com.mctg.requestHandler.RequestHandler;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

public class MTCGServer {
    private final int port;
    private final RequestHandler requestHandler;
    private final Map<String, String> activeTokens = new HashMap<>();

    public MTCGServer(int port, RequestHandler requestHandler) {
        this.port = port;
        this.requestHandler = requestHandler;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port: " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error handling client: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            HttpMethod method;
            try {
                method = HttpMethod.valueOf(requestParts[0]);
            } catch (IllegalArgumentException e) {
                sendErrorResponse(out, 501, "Not Implemented");
                return;
            }

            String path = requestParts[1];
            int contentLength = 0;
            StringBuilder body = new StringBuilder();
            Map<String, String> headers = new HashMap<>();  // Store headers

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                } else if (line.contains(":")) {
                    String[] headerParts = line.split(": ", 2);
                    headers.put(headerParts[0], headerParts[1]);
                }
            }

            if (contentLength > 0) {
                char[] buffer = new char[contentLength];
                in.read(buffer);
                body.append(buffer);
            }

            // Pass headers to Request constructor
            Request request = new Request(method, path, body.toString(), headers);
            String response = requestHandler.handleRequest(request);

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/plain");
            out.println("Content-Length: " + response.length());
            out.println();
            out.println(response);
        }
    }

    public String register(String username, String password) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO players (username, password_hash) VALUES (?, ?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));

            pstmt.executeUpdate();
            return "Registration successful!";

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {  // Unique constraint violation
                return "Username already exists.";
            }
            e.printStackTrace();
            return "Database error during registration.";
        }
    }

    public String login(String username, String password) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM players WHERE username = ?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password_hash");
                if (storedPassword.equals(hashPassword(password))) {
                    String token = generateToken(username);
                    activeTokens.put(token, username);
                    return "Login successful! Token: " + token;
                } else {
                    return "Invalid password.";
                }
            } else {
                return "User not found.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error during login.";
        }
    }

    private String hashPassword(String password) {
        // Simulate password hashing
        return String.valueOf(password.hashCode());
    }

    public boolean validateToken(String token) {
        return activeTokens.containsKey(token);
    }

    private String generateToken(String username) {
        return username + "-" + UUID.randomUUID().toString();
    }

    private void sendErrorResponse(PrintWriter out, int statusCode, String message) {
        out.println("HTTP/1.1 " + statusCode + " " + message);
        out.println("Content-Type: text/plain");
        out.println();
        out.println(message);
    }
}

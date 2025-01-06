package com.mctg.requestHandler;

import com.mctg.server.HttpMethod;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> body;
    private final Map<String, String> headers;  // Add headers field

    // Updated constructor to accept headers
    public Request(HttpMethod method, String path, String rawBody, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.body = parseJson(rawBody);
        this.headers = headers != null ? headers : new HashMap<>();  // Initialize headers
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getBody() {
        return body;
    }

    // New method to retrieve headers
    public String getHeader(String headerName) {
        return headers.getOrDefault(headerName, null);
    }

    // Existing JSON parsing method (no changes)
    private Map<String, String> parseJson(String json) {
        Map<String, String> data = new HashMap<>();
        if (json == null || json.isEmpty()) {
            System.out.println("Empty or null JSON received.");
            return data;
        }

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1).trim();
            String[] pairs = json.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"); // Handle commas within quotes
            for (String pair : pairs) {
                String[] keyValue = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                    String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                    data.put(key, value);
                }
            }
        } else {
            System.out.println("Malformed JSON: " + json);
        }
        return data;
    }
}

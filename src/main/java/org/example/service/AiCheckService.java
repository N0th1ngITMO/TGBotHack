package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AiCheckService {

    private final HttpClient client = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)   // <-- ключевая строка
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private final String endpoint = System.getenv().getOrDefault(
            "AI_MICROSERVICE_URL", "http://127.0.0.1:8060/check_ai");

    public CompletableFuture<Integer> sendText(String text) {

        String json = "{\"text\": \"" + text + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::extractIsHuman);
    }

    private Integer extractIsHuman(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            if (root.has("is_human")) {
                return root.get("is_human").asInt();
            } else {
                throw new IllegalStateException("No 'is_human' field in response: " + responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response from AI checker", e);
        }
    }

    // Экранирование текста на случай кавычек и спецсимволов
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

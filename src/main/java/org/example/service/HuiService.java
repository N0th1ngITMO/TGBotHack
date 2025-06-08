
package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.MessagePayload;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HuiService {

    private final HttpClient client = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)   // <-- ключевая строка
            .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint =
            System.getenv().getOrDefault("MICROSERVICE_URL",
                    "http://127.0.0.1:8070/check_hui");


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
            if (root.has("is_cursed")) {
                return root.get("is_cursed").asInt();
            } else {
                throw new IllegalStateException("No 'is_human' field in response: " + responseBody);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response from AI checker", e);
        }
    }
}

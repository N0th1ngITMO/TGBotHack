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

public class LinkService {

    private final HttpClient client = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)   // <-- ключевая строка
            .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final String endpoint =
            System.getenv().getOrDefault("MICROSERVICE_URL",
                    "http://127.0.0.1:8055/check_spam_ml");


    public CompletableFuture<Integer> sendPayload(String payload) throws IOException {
        String json = "{\"text\": \"" + payload + " \" }";
//        String json = mapper.writeValueAsString(payload); // пакуем пэйлод в джейсон
        // отправляем локально на сервере к нашему микросервису
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
// получаем ответ от сервера в виде числа например
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::extractResult);
    }
    // метод извлечения числа
    private Integer extractResult(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root.has("result")) {
                return root.get("result").asInt();
            }
            throw new IllegalStateException("No 'result' field in response: " + body);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse response: " + body, e);
        }
    }
}

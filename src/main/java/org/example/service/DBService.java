package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class DBService {

    private final HttpClient client = HttpClient
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1)   // <-- ключевая строка
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    private final String endpoint =
            System.getenv().getOrDefault("MICROSERVICE_URL",
                    "http://localhost:8000/items");


    public CompletableFuture<Double> userId(Long userId) throws IOException {

        String json = mapper.writeValueAsString(userId); // пакуем пэйлод в джейсон
        // отправляем локально на сервере к нашему микросервису
        String url = endpoint + "/" + userId;
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
// получаем ответ от сервера в виде числа например
        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::extractResult);
    }

    // метод извлечения числа
    private Double extractResult(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root.has("id")) {
                System.out.println(root.get("id").asDouble());
                return root.get("id").asDouble();
            }
            throw new IllegalStateException("No 'id' field in response: " + body);
        } catch (IOException e) {
            throw new RuntimeException("Can't parse microservice response: " + body, e);
        }
    }
}


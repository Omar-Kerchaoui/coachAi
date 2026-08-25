package com.lose10.ai.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lose10.ai.AIProviderException;
import com.lose10.ai.EmbeddingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Voyage AI is Anthropic's recommended embeddings partner (Claude itself
 * has no embeddings endpoint) — see docs/MEMORY.md §7. Same
 * raw-HTTP-over-Jackson approach as {@link ClaudeAIProvider}, for the same
 * reasons: no unproven SDK dependency, full control over the wire format.
 */
@Component
public class VoyageEmbeddingProvider implements EmbeddingProvider {

    private static final String API_URL = "https://api.voyageai.com/v1/embeddings";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int dimension;

    public VoyageEmbeddingProvider(
            ObjectMapper objectMapper,
            @Value("${lose10.ai.embedding.api-key:}") String apiKey,
            @Value("${lose10.ai.embedding.model:voyage-3.5}") String model,
            @Value("${lose10.ai.embedding.dimension:1024}") int dimension
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
    }

    @Override
    public Embedding generateEmbedding(String text) {
        return batchGenerateEmbeddings(List.of(text)).get(0);
    }

    @Override
    public List<Embedding> batchGenerateEmbeddings(List<String> texts) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("No embedding provider API key configured (lose10.ai.embedding.api-key)");
        }
        if (texts.isEmpty()) {
            return List.of();
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("output_dimension", dimension);
        ArrayNode input = root.putArray("input");
        texts.forEach(input::add);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AIProviderException("Voyage embeddings API returned " + response.statusCode() + ": " + response.body());
            }
            return parseResponse(response.body());
        } catch (AIProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AIProviderException("Voyage embeddings API call failed", e);
        }
    }

    private List<Embedding> parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        List<Embedding> embeddings = new ArrayList<>();
        for (JsonNode item : root.path("data")) {
            JsonNode vectorNode = item.path("embedding");
            float[] vector = new float[vectorNode.size()];
            for (int i = 0; i < vectorNode.size(); i++) {
                vector[i] = (float) vectorNode.get(i).asDouble();
            }
            embeddings.add(new Embedding(vector, model, vector.length));
        }
        return embeddings;
    }
}

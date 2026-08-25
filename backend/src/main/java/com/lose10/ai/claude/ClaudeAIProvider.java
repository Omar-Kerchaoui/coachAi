package com.lose10.ai.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lose10.ai.AIProvider;
import com.lose10.ai.AIProviderException;
import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;
import com.lose10.ai.dto.ContentBlock;
import com.lose10.ai.dto.Message;
import com.lose10.ai.dto.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The only class in the codebase permitted to talk to the Claude API
 * directly (docs/AI_ARCHITECTURE.md §1). Deliberately implemented against
 * the raw Messages API over {@link HttpClient} rather than a third-party
 * SDK — keeps the dependency footprint to what's already on the classpath
 * (Jackson) and the wire format fully under our control.
 */
@Component
public class ClaudeAIProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAIProvider.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String defaultModel;

    public ClaudeAIProvider(
            ObjectMapper objectMapper,
            @Value("${lose10.ai.claude.api-key:}") String apiKey,
            @Value("${lose10.ai.claude.model:claude-sonnet-5}") String defaultModel
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
    }

    @Override
    public AIResponse generate(AIRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("No Claude API key configured (lose10.ai.claude.api-key)");
        }

        String body = buildRequestBody(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        AIProviderException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return parseResponse(response.body());
                }
                if (isRetryable(response.statusCode()) && attempt < MAX_ATTEMPTS) {
                    log.warn("Claude API returned {} (attempt {}/{}), retrying", response.statusCode(), attempt, MAX_ATTEMPTS);
                    sleepBackoff(attempt);
                    continue;
                }
                throw new AIProviderException("Claude API returned " + response.statusCode() + ": " + response.body());
            } catch (AIProviderException e) {
                throw e;
            } catch (Exception e) {
                lastFailure = new AIProviderException("Claude API call failed (attempt " + attempt + "/" + MAX_ATTEMPTS + ")", e);
                if (attempt < MAX_ATTEMPTS) {
                    sleepBackoff(attempt);
                }
            }
        }
        throw lastFailure;
    }

    private boolean isRetryable(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(Duration.ofMillis(300L * (1L << attempt)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildRequestBody(AIRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", defaultModel);
        root.put("max_tokens", request.maxTokens());
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            root.put("system", request.systemPrompt());
        }

        ArrayNode messages = root.putArray("messages");
        for (Message message : request.messages()) {
            ObjectNode messageNode = messages.addObject();
            messageNode.put("role", message.role() == Message.Role.USER ? "user" : "assistant");
            ArrayNode contentArray = messageNode.putArray("content");
            for (ContentBlock block : message.content()) {
                contentArray.add(toJson(block));
            }
        }

        if (request.tools() != null && !request.tools().isEmpty()) {
            ArrayNode tools = root.putArray("tools");
            for (ToolDefinition tool : request.tools()) {
                ObjectNode toolNode = tools.addObject();
                toolNode.put("name", tool.name());
                toolNode.put("description", tool.description());
                toolNode.set("input_schema", objectMapper.valueToTree(tool.inputSchema()));
            }
        }

        return root.toString();
    }

    private ObjectNode toJson(ContentBlock block) {
        ObjectNode node = objectMapper.createObjectNode();
        switch (block) {
            case ContentBlock.TextBlock text -> {
                node.put("type", "text");
                node.put("text", text.text());
            }
            case ContentBlock.ToolUseBlock toolUse -> {
                node.put("type", "tool_use");
                node.put("id", toolUse.id());
                node.put("name", toolUse.name());
                node.set("input", objectMapper.valueToTree(toolUse.input()));
            }
            case ContentBlock.ToolResultBlock toolResult -> {
                node.put("type", "tool_result");
                node.put("tool_use_id", toolResult.toolUseId());
                node.put("content", toolResult.content());
                node.put("is_error", toolResult.isError());
            }
        }
        return node;
    }

    private AIResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<ContentBlock> content = new ArrayList<>();
            for (JsonNode block : root.path("content")) {
                String type = block.path("type").asText();
                switch (type) {
                    case "text" -> content.add(new ContentBlock.TextBlock(block.path("text").asText()));
                    case "tool_use" -> {
                        Map<String, Object> input = objectMapper.convertValue(block.path("input"), Map.class);
                        content.add(new ContentBlock.ToolUseBlock(block.path("id").asText(), block.path("name").asText(), input));
                    }
                    default -> log.debug("Ignoring unrecognized content block type: {}", type);
                }
            }

            AIResponse.StopReason stopReason = switch (root.path("stop_reason").asText("end_turn")) {
                case "tool_use" -> AIResponse.StopReason.TOOL_USE;
                case "max_tokens" -> AIResponse.StopReason.MAX_TOKENS;
                default -> AIResponse.StopReason.END_TURN;
            };

            return new AIResponse(
                    content,
                    stopReason,
                    root.path("usage").path("input_tokens").asInt(0),
                    root.path("usage").path("output_tokens").asInt(0),
                    root.path("model").asText(defaultModel)
            );
        } catch (Exception e) {
            throw new AIProviderException("Failed to parse Claude API response", e);
        }
    }
}

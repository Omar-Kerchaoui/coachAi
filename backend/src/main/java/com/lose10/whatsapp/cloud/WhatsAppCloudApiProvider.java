package com.lose10.whatsapp.cloud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lose10.whatsapp.WhatsAppProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * The only class permitted to talk to the Meta Graph API directly
 * (docs/WHATSAPP.md). Same raw-HTTP-over-Jackson approach as
 * {@code ClaudeAIProvider} for the same reasons — no SDK dependency, full
 * control over the wire format.
 */
@Component
public class WhatsAppCloudApiProvider implements WhatsAppProvider {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudApiProvider.class);
    private static final String GRAPH_API_VERSION = "v20.0";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final String phoneNumberId;

    public WhatsAppCloudApiProvider(
            ObjectMapper objectMapper,
            @Value("${lose10.whatsapp.access-token:}") String accessToken,
            @Value("${lose10.whatsapp.phone-number-id:}") String phoneNumberId
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
    }

    @Override
    public SendResult sendText(String waId, String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("messaging_product", "whatsapp");
        body.put("to", waId);
        body.put("type", "text");
        body.putObject("text").put("body", text);
        return send(body);
    }

    @Override
    public SendResult sendTemplate(String waId, String templateName, Map<String, Object> templateParams) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("messaging_product", "whatsapp");
        body.put("to", waId);
        body.put("type", "template");
        ObjectNode template = body.putObject("template");
        template.put("name", templateName);
        template.putObject("language").put("code", "en_US");
        if (templateParams != null && !templateParams.isEmpty()) {
            template.set("components", objectMapper.valueToTree(templateParams));
        }
        return send(body);
    }

    private SendResult send(ObjectNode body) {
        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            return SendResult.failure("WhatsApp credentials not configured");
        }
        String url = "https://graph.facebook.com/" + GRAPH_API_VERSION + "/" + phoneNumberId + "/messages";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + accessToken)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("WhatsApp send failed: {} {}", response.statusCode(), response.body());
                return SendResult.failure("WhatsApp API returned " + response.statusCode());
            }
            JsonNode json = objectMapper.readTree(response.body());
            String waMessageId = json.path("messages").path(0).path("id").asText(null);
            return SendResult.ok(waMessageId);
        } catch (Exception e) {
            log.error("WhatsApp send threw an exception", e);
            return SendResult.failure(e.getMessage());
        }
    }
}

package com.lose10.subscription.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.subscription.StripeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The only class permitted to talk to the Stripe REST API directly. Same
 * raw-HTTP-over-Jackson approach as {@code ClaudeAIProvider} and
 * {@code WhatsAppCloudApiProvider} — no SDK dependency. Stripe's API takes
 * {@code application/x-www-form-urlencoded} request bodies (including for
 * nested params like {@code line_items[0][price]}), unlike Claude/WhatsApp's
 * JSON bodies — this is the one place in the codebase building form-encoded
 * requests instead.
 */
@Component
public class StripeApiProvider implements StripeProvider {

    private static final Logger log = LoggerFactory.getLogger(StripeApiProvider.class);
    private static final String BASE_URL = "https://api.stripe.com/v1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public StripeApiProvider(ObjectMapper objectMapper, @Value("${lose10.stripe.secret-key:}") String secretKey) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
    }

    @Override
    public Result<String> createCustomer(UUID userId, String email) {
        if (!configured()) {
            return Result.failure("Stripe not configured");
        }
        List<String> params = new ArrayList<>();
        params.add(param("email", email));
        params.add(param("metadata[user_id]", userId.toString()));
        return post("/customers", params).map(json -> json.path("id").asText(null));
    }

    @Override
    public Result<CheckoutSession> createCheckoutSession(UUID userId, String customerId, String priceId, int trialDays, String couponId, String successUrl, String cancelUrl) {
        if (!configured()) {
            return Result.failure("Stripe not configured");
        }
        List<String> params = new ArrayList<>();
        params.add(param("mode", "subscription"));
        params.add(param("customer", customerId));
        params.add(param("line_items[0][price]", priceId));
        params.add(param("line_items[0][quantity]", "1"));
        params.add(param("success_url", successUrl));
        params.add(param("cancel_url", cancelUrl));
        params.add(param("metadata[user_id]", userId.toString()));
        params.add(param("subscription_data[metadata][user_id]", userId.toString()));
        if (trialDays > 0) {
            params.add(param("subscription_data[trial_period_days]", String.valueOf(trialDays)));
        }
        if (couponId != null && !couponId.isBlank()) {
            params.add(param("discounts[0][coupon]", couponId));
        }
        Result<JsonNode> response = post("/checkout/sessions", params);
        if (!response.success()) {
            return Result.failure(response.errorMessage());
        }
        JsonNode json = response.value();
        return Result.ok(new CheckoutSession(json.path("id").asText(null), json.path("url").asText(null)));
    }

    @Override
    public Result<String> createPortalSession(String customerId, String returnUrl) {
        if (!configured()) {
            return Result.failure("Stripe not configured");
        }
        List<String> params = new ArrayList<>();
        params.add(param("customer", customerId));
        params.add(param("return_url", returnUrl));
        return post("/billing_portal/sessions", params).map(json -> json.path("url").asText(null));
    }

    @Override
    public Result<Void> cancelAtPeriodEnd(String stripeSubscriptionId) {
        if (!configured()) {
            return Result.failure("Stripe not configured");
        }
        List<String> params = new ArrayList<>();
        params.add(param("cancel_at_period_end", "true"));
        Result<JsonNode> response = post("/subscriptions/" + stripeSubscriptionId, params);
        return response.success() ? Result.ok(null) : Result.failure(response.errorMessage());
    }

    private boolean configured() {
        return secretKey != null && !secretKey.isBlank();
    }

    private String param(String key, String value) {
        return key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private Result<JsonNode> post(String path, List<String> formParams) {
        String body = formParams.stream().collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + secretKey)
                .header("content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() >= 400) {
                String message = json.path("error").path("message").asText("Stripe API returned " + response.statusCode());
                log.error("Stripe API call to {} failed: {}", path, message);
                return Result.failure(message);
            }
            return Result.ok(json);
        } catch (Exception e) {
            log.error("Stripe API call to {} threw an exception", path, e);
            return Result.failure(e.getMessage());
        }
    }
}

package com.lose10.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Public, unauthenticated by design (permitted in SecurityConfig) — trust
 * comes from the HMAC signature on the raw body, not a bearer token
 * (docs/backlog L10-804).
 */
@RestController
@RequestMapping("/api/v1/stripe/webhook")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookSignatureVerifier signatureVerifier;
    private final SubscriptionWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(StripeWebhookSignatureVerifier signatureVerifier, SubscriptionWebhookService webhookService, ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        if (!signatureVerifier.isValid(rawBody.getBytes(StandardCharsets.UTF_8), signature)) {
            log.warn("Rejected Stripe webhook with invalid signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            webhookService.process(payload);
        } catch (Exception e) {
            log.error("Failed to parse/process Stripe webhook body", e);
        }
        // Always 200 once the signature is valid — Stripe retries on
        // non-2xx, and processing failures are handled/recorded internally.
        return ResponseEntity.ok().build();
    }
}

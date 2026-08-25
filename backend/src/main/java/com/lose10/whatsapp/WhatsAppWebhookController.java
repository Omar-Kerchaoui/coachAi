package com.lose10.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Public, unauthenticated by design (permitted in SecurityConfig) — trust
 * comes from the verify token (GET handshake) and the HMAC signature
 * (POST), not a bearer token (docs/WHATSAPP.md, docs/SECURITY.md §5).
 */
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WebhookSignatureVerifier signatureVerifier;
    private final WhatsAppWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final String verifyToken;

    public WhatsAppWebhookController(
            WebhookSignatureVerifier signatureVerifier,
            WhatsAppWebhookService webhookService,
            ObjectMapper objectMapper,
            @Value("${lose10.whatsapp.verify-token:}") String verifyToken
    ) {
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(mode) && !verifyToken.isBlank() && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature
    ) {
        if (!signatureVerifier.isValid(rawBody.getBytes(StandardCharsets.UTF_8), signature)) {
            log.warn("Rejected WhatsApp webhook with invalid signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            JsonNode payload = objectMapper.readTree(rawBody);
            webhookService.process(payload);
        } catch (Exception e) {
            log.error("Failed to parse/process WhatsApp webhook body", e);
        }
        // Always 200 once the signature is valid — Meta retries on non-2xx,
        // and processing failures are handled internally, not surfaced here.
        return ResponseEntity.ok().build();
    }
}

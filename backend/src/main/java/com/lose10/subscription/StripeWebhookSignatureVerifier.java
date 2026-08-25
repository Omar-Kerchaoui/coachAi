package com.lose10.subscription;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifies Stripe's {@code Stripe-Signature} header. Unlike Meta's single
 * hex digest, Stripe's format is {@code t=<timestamp>,v1=<sig>[,v1=<sig>...]}
 * — the signed payload is {@code "<timestamp>.<raw body>"}, HMAC-SHA256'd
 * with the webhook signing secret. Multiple {@code v1} entries occur during
 * Stripe's own secret rotation; any one matching is sufficient. A tolerance
 * window (Stripe's own default is 5 minutes) rejects stale/replayed
 * deliveries even if the signature is otherwise valid.
 */
@Component
public class StripeWebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Duration TOLERANCE = Duration.ofMinutes(5);

    private final String webhookSecret;

    public StripeWebhookSignatureVerifier(@Value("${lose10.stripe.webhook-signing-secret:}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(byte[] rawBody, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeader == null) {
            return false;
        }
        String timestamp = null;
        String v1Signature = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0])) {
                timestamp = kv[1];
            } else if ("v1".equals(kv[0]) && v1Signature == null) {
                v1Signature = kv[1];
            }
        }
        if (timestamp == null || v1Signature == null) {
            return false;
        }
        if (!withinTolerance(timestamp)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            String signedPayload = timestamp + "." + new String(rawBody, StandardCharsets.UTF_8);
            byte[] computed = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.US_ASCII), v1Signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean withinTolerance(String timestamp) {
        try {
            Instant eventTime = Instant.ofEpochSecond(Long.parseLong(timestamp));
            Duration age = Duration.between(eventTime, Instant.now()).abs();
            return age.compareTo(TOLERANCE) <= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

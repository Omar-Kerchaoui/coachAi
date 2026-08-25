package com.lose10.subscription;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class StripeWebhookSignatureVerifierTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private final StripeWebhookSignatureVerifier verifier = new StripeWebhookSignatureVerifier(WEBHOOK_SECRET);

    @Test
    void acceptsACorrectlySignedPayload() throws Exception {
        String body = "{\"id\":\"evt_1\",\"type\":\"customer.subscription.created\"}";
        String header = signedHeader(WEBHOOK_SECRET, body, Instant.now());

        assertThat(verifier.isValid(body.getBytes(StandardCharsets.UTF_8), header)).isTrue();
    }

    @Test
    void rejectsATamperedPayload() throws Exception {
        String originalBody = "{\"id\":\"evt_1\",\"type\":\"customer.subscription.created\"}";
        String header = signedHeader(WEBHOOK_SECRET, originalBody, Instant.now());
        String tamperedBody = "{\"id\":\"evt_1\",\"type\":\"customer.subscription.deleted\"}";

        assertThat(verifier.isValid(tamperedBody.getBytes(StandardCharsets.UTF_8), header)).isFalse();
    }

    @Test
    void rejectsAMissingSignatureHeader() {
        assertThat(verifier.isValid("{}".getBytes(StandardCharsets.UTF_8), null)).isFalse();
    }

    @Test
    void rejectsASignatureFromTheWrongSecret() throws Exception {
        String body = "{\"id\":\"evt_1\"}";
        String header = signedHeader("whsec_a_different_secret", body, Instant.now());

        assertThat(verifier.isValid(body.getBytes(StandardCharsets.UTF_8), header)).isFalse();
    }

    @Test
    void rejectsWhenNoWebhookSecretIsConfigured() throws Exception {
        StripeWebhookSignatureVerifier unconfigured = new StripeWebhookSignatureVerifier("");
        String body = "{\"id\":\"evt_1\"}";
        String header = signedHeader(WEBHOOK_SECRET, body, Instant.now());

        assertThat(unconfigured.isValid(body.getBytes(StandardCharsets.UTF_8), header)).isFalse();
    }

    @Test
    void rejectsAStaleTimestampOutsideTheToleranceWindow() throws Exception {
        String body = "{\"id\":\"evt_1\"}";
        String header = signedHeader(WEBHOOK_SECRET, body, Instant.now().minusSeconds(600));

        assertThat(verifier.isValid(body.getBytes(StandardCharsets.UTF_8), header)).isFalse();
    }

    private String signedHeader(String secret, String body, Instant timestamp) throws Exception {
        long t = timestamp.getEpochSecond();
        String signedPayload = t + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=" + t + ",v1=" + sig;
    }
}

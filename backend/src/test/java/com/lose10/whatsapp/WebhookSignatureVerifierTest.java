package com.lose10.whatsapp;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private static final String APP_SECRET = "test-app-secret";
    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(APP_SECRET);

    @Test
    void acceptsACorrectlySignedPayload() throws Exception {
        String body = "{\"entry\":[]}";
        String signature = "sha256=" + hmacSha256Hex(APP_SECRET, body);

        assertThat(verifier.isValid(body.getBytes(StandardCharsets.UTF_8), signature)).isTrue();
    }

    @Test
    void rejectsATamperedPayload() throws Exception {
        String originalBody = "{\"entry\":[]}";
        String signature = "sha256=" + hmacSha256Hex(APP_SECRET, originalBody);
        String tamperedBody = "{\"entry\":[{\"injected\":true}]}";

        assertThat(verifier.isValid(tamperedBody.getBytes(StandardCharsets.UTF_8), signature)).isFalse();
    }

    @Test
    void rejectsAMissingSignatureHeader() {
        assertThat(verifier.isValid("{}".getBytes(StandardCharsets.UTF_8), null)).isFalse();
    }

    @Test
    void rejectsASignatureFromTheWrongSecret() throws Exception {
        String body = "{\"entry\":[]}";
        String signature = "sha256=" + hmacSha256Hex("a-completely-different-secret", body);

        assertThat(verifier.isValid(body.getBytes(StandardCharsets.UTF_8), signature)).isFalse();
    }

    @Test
    void rejectsWhenNoAppSecretIsConfigured() throws Exception {
        WebhookSignatureVerifier unconfigured = new WebhookSignatureVerifier("");
        String body = "{\"entry\":[]}";
        String signature = "sha256=" + hmacSha256Hex(APP_SECRET, body);

        assertThat(unconfigured.isValid(body.getBytes(StandardCharsets.UTF_8), signature)).isFalse();
    }

    private String hmacSha256Hex(String secret, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}

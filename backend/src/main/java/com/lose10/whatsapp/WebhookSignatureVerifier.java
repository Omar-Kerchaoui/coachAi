package com.lose10.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies Meta's {@code X-Hub-Signature-256} header — HMAC-SHA256 of the
 * raw request body using the app secret (docs/SECURITY.md §5). Runs on the
 * exact raw bytes, before any JSON parsing; a body re-serialized by a JSON
 * library would not reproduce the same signature.
 */
@Component
public class WebhookSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String appSecret;

    public WebhookSignatureVerifier(@Value("${lose10.whatsapp.app-secret:}") String appSecret) {
        this.appSecret = appSecret;
    }

    public boolean isValid(byte[] rawBody, String signatureHeader) {
        if (appSecret == null || appSecret.isBlank() || signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] computed = mac.doFinal(rawBody);
            String computedHex = HexFormat.of().formatHex(computed);
            String providedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
            return MessageDigest.isEqual(computedHex.getBytes(StandardCharsets.US_ASCII), providedHex.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }
}

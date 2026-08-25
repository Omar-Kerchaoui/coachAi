package com.lose10.whatsapp;

import java.util.Map;

/**
 * The only interface the rest of the codebase uses to talk to WhatsApp —
 * no other module references the Meta Graph API directly
 * (docs/WHATSAPP.md). Signature verification lives separately in
 * {@link WebhookSignatureVerifier} since it's a webhook-receiving concern,
 * not a send-side one.
 */
public interface WhatsAppProvider {

    SendResult sendText(String waId, String text);

    SendResult sendTemplate(String waId, String templateName, Map<String, Object> templateParams);

    record SendResult(boolean success, String waMessageId, String errorMessage) {
        public static SendResult ok(String waMessageId) {
            return new SendResult(true, waMessageId, null);
        }

        public static SendResult failure(String errorMessage) {
            return new SendResult(false, null, errorMessage);
        }
    }
}

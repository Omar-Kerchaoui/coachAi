package com.lose10.analytics;

import com.lose10.whatsapp.WhatsAppMessageType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Illustrative USD-per-message pricing reflecting the *structure* of Meta's
 * real WhatsApp Business pricing (docs/backlog L10-912): a template message
 * opens a new priced conversation window, while a session (free-form) reply
 * within an existing 24-hour customer-service window is materially cheaper.
 * Meta's actual rates vary by recipient country and conversation category
 * (marketing/utility/authentication/service) — real per-country rates
 * should replace these constants once a live WhatsApp Business Account is
 * connected; the point here is the template-vs-session split itself, not
 * these exact numbers.
 */
@Component
public class WhatsAppCostEstimator {

    private static final BigDecimal TEMPLATE_MESSAGE_COST_USD = BigDecimal.valueOf(0.0500);
    private static final BigDecimal SESSION_MESSAGE_COST_USD = BigDecimal.valueOf(0.0080);

    public BigDecimal costFor(WhatsAppMessageType type) {
        return type == WhatsAppMessageType.TEMPLATE ? TEMPLATE_MESSAGE_COST_USD : SESSION_MESSAGE_COST_USD;
    }
}

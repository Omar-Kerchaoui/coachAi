package com.lose10.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatches Stripe webhook events by type (docs/backlog L10-805, L10-806).
 * Each handler delegates its actual field-level state change to
 * {@link SubscriptionService}, which is the sole owner of subscription
 * status writes. Event recording uses the same short-independent-
 * transaction split as {@code WhatsAppWebhookEventRecorder}
 * (see {@link SubscriptionEventRecorder}).
 */
@Service
public class SubscriptionWebhookService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionWebhookService.class);

    private final SubscriptionEventRecorder eventRecorder;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    public SubscriptionWebhookService(SubscriptionEventRecorder eventRecorder, SubscriptionService subscriptionService, ObjectMapper objectMapper) {
        this.eventRecorder = eventRecorder;
        this.subscriptionService = subscriptionService;
        this.objectMapper = objectMapper;
    }

    public void process(JsonNode payload) {
        String stripeEventId = payload.path("id").asText(null);
        String eventType = payload.path("type").asText(null);
        if (stripeEventId == null || eventType == null) {
            log.warn("Rejected Stripe webhook payload missing id/type");
            return;
        }
        if (eventRecorder.isAlreadyProcessed(stripeEventId)) {
            log.debug("Stripe event {} already processed — skipping", stripeEventId);
            return;
        }
        UUID eventRecordId = eventRecorder.recordReceived(stripeEventId, eventType, toMap(payload));

        boolean success = true;
        try {
            JsonNode object = payload.path("data").path("object");
            switch (eventType) {
                case "customer.subscription.created", "customer.subscription.updated", "customer.subscription.deleted" -> {
                    UUID subscriptionId = handleSubscriptionLifecycle(object);
                    eventRecorder.linkSubscription(eventRecordId, subscriptionId);
                }
                case "customer.subscription.trial_will_end" ->
                        log.info("Trial ending soon for Stripe subscription {}", object.path("id").asText());
                case "invoice.paid" -> handleInvoicePaid(object);
                case "invoice.payment_failed" -> handleInvoicePaymentFailed(object);
                case "charge.refunded" -> handleChargeRefunded(object);
                default -> log.debug("Ignoring unhandled Stripe event type {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process Stripe webhook event {} ({})", stripeEventId, eventType, e);
            success = false;
        }
        eventRecorder.markOutcome(eventRecordId, success);
    }

    private UUID handleSubscriptionLifecycle(JsonNode subscription) {
        String customerId = subscription.path("customer").asText(null);
        String subscriptionId = subscription.path("id").asText(null);
        String status = subscription.path("status").asText(null);
        Instant trialEnd = epochSecondsOrNull(subscription.path("trial_end"));
        Instant currentPeriodEnd = epochSecondsOrNull(subscription.path("current_period_end"));
        boolean cancelAtPeriodEnd = subscription.path("cancel_at_period_end").asBoolean(false);
        String priceId = subscription.path("items").path("data").path(0).path("price").path("id").asText(null);

        return subscriptionService.applySubscriptionLifecycleEvent(customerId, subscriptionId, status, trialEnd, currentPeriodEnd, cancelAtPeriodEnd, priceId);
    }

    private void handleInvoicePaid(JsonNode invoice) {
        String subscriptionId = invoice.path("subscription").asText(null);
        String paymentIntentId = invoice.path("payment_intent").asText(null);
        int amountCents = invoice.path("amount_paid").asInt(0);
        String currency = invoice.path("currency").asText("usd");
        if (subscriptionId == null || paymentIntentId == null) {
            return; // one-off invoice items not tied to a subscription payment — out of scope
        }
        subscriptionService.recordPayment(subscriptionId, paymentIntentId, amountCents, currency, PaymentStatus.SUCCEEDED);
    }

    private void handleInvoicePaymentFailed(JsonNode invoice) {
        String subscriptionId = invoice.path("subscription").asText(null);
        String paymentIntentId = invoice.path("payment_intent").asText(null);
        int amountCents = invoice.path("amount_due").asInt(0);
        String currency = invoice.path("currency").asText("usd");
        if (subscriptionId == null || paymentIntentId == null) {
            return;
        }
        // Status progression toward past_due/canceled is driven entirely by
        // Stripe's own Smart Retries via the subsequent
        // customer.subscription.updated event — this only records the
        // failed charge itself (docs/backlog L10-806).
        subscriptionService.recordPayment(subscriptionId, paymentIntentId, amountCents, currency, PaymentStatus.FAILED);
    }

    private void handleChargeRefunded(JsonNode charge) {
        String paymentIntentId = charge.path("payment_intent").asText(null);
        if (paymentIntentId == null) {
            return;
        }
        for (JsonNode refund : charge.path("refunds").path("data")) {
            String refundId = refund.path("id").asText(null);
            int amountCents = refund.path("amount").asInt(0);
            String reason = refund.path("reason").asText(null);
            if (refundId != null) {
                subscriptionService.recordRefund(paymentIntentId, refundId, amountCents, reason);
            }
        }
    }

    private Instant epochSecondsOrNull(JsonNode node) {
        return node.isNumber() ? Instant.ofEpochSecond(node.asLong()) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode payload) {
        return objectMapper.convertValue(payload, Map.class);
    }
}

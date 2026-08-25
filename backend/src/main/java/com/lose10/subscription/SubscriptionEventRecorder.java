package com.lose10.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Short, independent transactions for idempotency bookkeeping around Stripe
 * webhook processing — same split as {@code WhatsAppWebhookEventRecorder}
 * and for the same reason: keeping these out of one big ambient transaction
 * means a gracefully-handled "not found" from another module's
 * {@code @Transactional} method can never poison this event's own
 * bookkeeping.
 */
@Service
public class SubscriptionEventRecorder {

    private final SubscriptionEventRepository repository;

    public SubscriptionEventRecorder(SubscriptionEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String stripeEventId) {
        return repository.findByStripeEventId(stripeEventId).isPresent();
    }

    @Transactional
    public UUID recordReceived(String stripeEventId, String eventType, Map<String, Object> payload) {
        return repository.save(new SubscriptionEvent(stripeEventId, eventType, payload)).getId();
    }

    @Transactional
    public void linkSubscription(UUID eventRecordId, UUID subscriptionId) {
        repository.findById(eventRecordId).ifPresent(e -> e.linkSubscription(subscriptionId));
    }

    @Transactional
    public void markOutcome(UUID eventRecordId, boolean success) {
        repository.findById(eventRecordId).ifPresent(e -> {
            if (success) {
                e.markProcessed();
            } else {
                e.markFailed();
            }
        });
    }
}

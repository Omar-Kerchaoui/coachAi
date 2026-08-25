package com.lose10.whatsapp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Split out from {@link WhatsAppWebhookService} so recording an event's
 * start/outcome happens in its own short transactions, invoked through a
 * real Spring proxy. This matters here specifically: if the whole webhook
 * handling chain (including the AI orchestrator call) ran inside one
 * ambient transaction, any inner {@code @Transactional} method that throws
 * — even one whose exception is caught and handled gracefully one frame up
 * (e.g. ContextBuilder catching "no active goal yet") — marks that entire
 * ambient transaction rollback-only. Spring's transactional AOP proxy does
 * this at the point the exception leaves the inner method, regardless of
 * what the caller does with it afterward, so the final commit then fails
 * with UnexpectedRollbackException even though every individual step
 * "succeeded." Caught via a live webhook test, not by inspection.
 */
@Service
public class WhatsAppWebhookEventRecorder {

    private final WhatsAppWebhookEventRepository repository;

    public WhatsAppWebhookEventRecorder(WhatsAppWebhookEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String eventId) {
        return repository.findByEventId(eventId).isPresent();
    }

    @Transactional
    public UUID recordReceived(String eventId, Map<String, Object> payload) {
        return repository.save(new WhatsAppWebhookEvent(eventId, payload)).getId();
    }

    @Transactional
    public void markOutcome(UUID eventRecordId, boolean success) {
        Optional<WhatsAppWebhookEvent> event = repository.findById(eventRecordId);
        event.ifPresent(e -> {
            if (success) {
                e.markProcessed();
            } else {
                e.markFailed();
            }
        });
    }
}

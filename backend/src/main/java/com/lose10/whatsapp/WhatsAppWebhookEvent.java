package com.lose10.whatsapp;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Every webhook delivery recorded before processing, keyed by Meta's event
 * id — dedupes Meta's at-least-once delivery (docs/WHATSAPP.md §1).
 */
@Entity
@Table(name = "whatsapp_webhook_events")
public class WhatsAppWebhookEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 200)
    private String eventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus = "PENDING";

    protected WhatsAppWebhookEvent() {
    }

    public WhatsAppWebhookEvent(String eventId, Map<String, Object> payload) {
        this.eventId = eventId;
        this.payload = payload;
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
        this.processingStatus = "PROCESSED";
    }

    public void markFailed() {
        this.processingStatus = "FAILED";
    }

    public String getEventId() {
        return eventId;
    }
}

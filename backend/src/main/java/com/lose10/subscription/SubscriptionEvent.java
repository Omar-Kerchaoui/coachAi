package com.lose10.subscription;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "subscription_events")
public class SubscriptionEvent extends BaseEntity {

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "stripe_event_id", nullable = false, length = 255)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "processing_status", nullable = false, length = 20)
    private String processingStatus = "PENDING";

    @Column(name = "processed_at")
    private Instant processedAt;

    protected SubscriptionEvent() {
    }

    public SubscriptionEvent(String stripeEventId, String eventType, Map<String, Object> payload) {
        this.stripeEventId = stripeEventId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public void linkSubscription(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void markProcessed() {
        this.processingStatus = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.processingStatus = "FAILED";
        this.processedAt = Instant.now();
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public String getEventType() {
        return eventType;
    }
}

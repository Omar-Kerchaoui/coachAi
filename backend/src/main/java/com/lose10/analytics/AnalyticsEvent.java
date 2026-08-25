package com.lose10.analytics;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AnalyticsEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected AnalyticsEvent() {
    }

    public AnalyticsEvent(UUID userId, AnalyticsEventType eventType, Map<String, Object> metadata) {
        this.userId = userId;
        this.eventType = eventType;
        this.metadata = metadata;
    }

    public UUID getUserId() {
        return userId;
    }

    public AnalyticsEventType getEventType() {
        return eventType;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

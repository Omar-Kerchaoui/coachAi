package com.lose10.behavior;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * A raw signal the Behavior Engine aggregates from — recorded by whichever
 * domain module observes it (e.g. training records MISSED_WORKOUT). This
 * module only owns the aggregation, not the events themselves
 * (docs/MEMORY.md §10).
 */
@Entity
@Table(name = "behavior_events")
public class BehaviorEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pattern_id")
    private UUID patternId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    protected BehaviorEvent() {
    }

    public BehaviorEvent(UUID userId, String eventType, Map<String, Object> payload) {
        this.userId = userId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEventType() {
        return eventType;
    }

    public java.time.Instant occurredAt() {
        return getCreatedAt();
    }
}

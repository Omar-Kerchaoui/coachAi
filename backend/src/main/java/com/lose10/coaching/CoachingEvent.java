package com.lose10.coaching;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "coaching_events")
public class CoachingEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false, length = 10)
    private String actor = "SYSTEM";

    protected CoachingEvent() {
    }

    public CoachingEvent(UUID userId, String eventType, Map<String, Object> payload, String actor) {
        this.userId = userId;
        this.eventType = eventType;
        this.payload = payload;
        this.actor = actor;
    }
}

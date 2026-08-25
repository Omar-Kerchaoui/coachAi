package com.lose10.goal;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Audit trail of goal edits (target/date changes, pauses, etc.) — every
 * mutation to a goal after creation writes one of these (docs/DATABASE.md).
 */
@Entity
@Table(name = "goal_events")
public class GoalEvent extends BaseEntity {

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    protected GoalEvent() {
    }

    public GoalEvent(UUID goalId, String eventType, Map<String, Object> payload) {
        this.goalId = goalId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public String getEventType() {
        return eventType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}

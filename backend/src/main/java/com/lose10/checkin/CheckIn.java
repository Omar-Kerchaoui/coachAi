package com.lose10.checkin;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "check_ins")
public class CheckIn extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_type", nullable = false, length = 20)
    private CheckInType checkInType;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    @Column(name = "completed_at")
    private Instant completedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> responses;

    protected CheckIn() {
    }

    public CheckIn(UUID userId, CheckInType checkInType, Instant scheduledFor) {
        this.userId = userId;
        this.checkInType = checkInType;
        this.scheduledFor = scheduledFor;
    }

    public void complete(Map<String, Object> responses) {
        this.completedAt = Instant.now();
        this.responses = responses;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public UUID getUserId() {
        return userId;
    }

    public CheckInType getCheckInType() {
        return checkInType;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Map<String, Object> getResponses() {
        return responses;
    }
}

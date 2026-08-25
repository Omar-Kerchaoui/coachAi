package com.lose10.notification;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType;

    @Column(name = "scheduled_for", nullable = false)
    private Instant scheduledFor;

    /**
     * The user's local calendar date at {@link #scheduledFor}, computed by
     * the application — see the migration's comment for why this can't be
     * a Postgres index expression on scheduled_for directly.
     */
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    protected ScheduledMessage() {
    }

    public ScheduledMessage(UUID userId, MessageType messageType, Instant scheduledFor, ZoneId userZone, Map<String, Object> payload) {
        this.userId = userId;
        this.messageType = messageType;
        this.scheduledFor = scheduledFor;
        this.scheduledDate = scheduledFor.atZone(userZone).toLocalDate();
        this.payload = payload;
    }

    public void markSent() {
        this.status = "SENT";
    }

    public void markSkipped() {
        this.status = "SKIPPED";
    }

    public void markFailed() {
        this.status = "FAILED";
    }

    public UUID getUserId() {
        return userId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getStatus() {
        return status;
    }
}

package com.lose10.notification;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "notification_events")
public class NotificationEvent extends BaseEntity {

    @Column(name = "scheduled_message_id", nullable = false)
    private UUID scheduledMessageId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    protected NotificationEvent() {
    }

    public NotificationEvent(UUID scheduledMessageId, String eventType) {
        this.scheduledMessageId = scheduledMessageId;
        this.eventType = eventType;
    }
}

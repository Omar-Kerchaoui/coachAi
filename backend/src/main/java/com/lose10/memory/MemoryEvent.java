package com.lose10.memory;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "memory_events")
public class MemoryEvent extends BaseEntity {

    public enum EventType {
        CREATED, UPDATED, CONTRADICTED, EXPIRED, DELETED
    }

    @Column(name = "memory_id", nullable = false)
    private UUID memoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "previous_content", columnDefinition = "text")
    private String previousContent;

    protected MemoryEvent() {
    }

    public MemoryEvent(UUID memoryId, EventType eventType, String previousContent) {
        this.memoryId = memoryId;
        this.eventType = eventType;
        this.previousContent = previousContent;
    }
}

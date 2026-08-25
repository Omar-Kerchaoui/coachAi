package com.lose10.admin.dto;

import com.lose10.ai.safety.SafetyCategory;
import com.lose10.ai.safety.SafetySeverity;
import com.lose10.safety.SafetyEvent;

import java.time.Instant;
import java.util.UUID;

public record SafetyEventResponse(UUID id, UUID userId, SafetyCategory category, SafetySeverity severity, boolean resolved, Instant createdAt) {
    public static SafetyEventResponse from(SafetyEvent event) {
        return new SafetyEventResponse(event.getId(), event.getUserId(), event.getCategory(), event.getSeverity(), event.isResolved(), event.getCreatedAt());
    }
}

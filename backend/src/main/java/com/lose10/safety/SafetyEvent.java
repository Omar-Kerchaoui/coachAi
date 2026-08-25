package com.lose10.safety;

import com.lose10.ai.safety.SafetyCategory;
import com.lose10.ai.safety.SafetySeverity;
import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "safety_events")
public class SafetyEvent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SafetyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SafetySeverity severity;

    @Column(name = "detected_by", nullable = false, length = 20)
    private String detectedBy = "RULE";

    @Column(name = "source_text")
    private String sourceText;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column
    private String resolution;

    protected SafetyEvent() {
    }

    public SafetyEvent(UUID userId, SafetyCategory category, SafetySeverity severity, String detectedBy, String sourceText) {
        this.userId = userId;
        this.category = category;
        this.severity = severity;
        this.detectedBy = detectedBy;
        this.sourceText = sourceText;
    }

    public void resolve(UUID adminId, String resolution) {
        this.resolved = true;
        this.resolvedAt = Instant.now();
        this.resolvedBy = adminId;
        this.resolution = resolution;
    }

    public UUID getUserId() {
        return userId;
    }

    public SafetyCategory getCategory() {
        return category;
    }

    public SafetySeverity getSeverity() {
        return severity;
    }

    public boolean isResolved() {
        return resolved;
    }
}

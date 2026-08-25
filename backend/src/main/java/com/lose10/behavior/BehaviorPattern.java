package com.lose10.behavior;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "behavior_patterns")
public class BehaviorPattern extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pattern_key", nullable = false, length = 100)
    private String patternKey;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private BigDecimal confidence;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "first_observed_at", nullable = false)
    private Instant firstObservedAt;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    protected BehaviorPattern() {
    }

    public BehaviorPattern(UUID userId, String patternKey, String description, BigDecimal confidence) {
        this.userId = userId;
        this.patternKey = patternKey;
        this.description = description;
        this.confidence = confidence;
        this.occurrenceCount = 1;
        this.firstObservedAt = Instant.now();
        this.lastObservedAt = Instant.now();
    }

    public void reinforce(BigDecimal newConfidence) {
        this.occurrenceCount++;
        this.lastObservedAt = Instant.now();
        this.confidence = newConfidence;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPatternKey() {
        return patternKey;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }
}

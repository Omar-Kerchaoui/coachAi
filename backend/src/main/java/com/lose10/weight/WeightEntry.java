package com.lose10.weight;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weight_entries")
public class WeightEntry extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WeightSource source = WeightSource.USER_REPORTED;

    protected WeightEntry() {
    }

    public WeightEntry(UUID userId, BigDecimal weightKg, Instant recordedAt, WeightSource source) {
        this.userId = userId;
        this.weightKg = weightKg;
        this.recordedAt = recordedAt;
        this.source = source;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public WeightSource getSource() {
        return source;
    }
}

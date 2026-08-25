package com.lose10.weight;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "body_measurements")
public class BodyMeasurement extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "measurement_type", nullable = false, length = 30)
    private String measurementType;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(nullable = false, length = 10)
    private String unit;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected BodyMeasurement() {
    }

    public BodyMeasurement(UUID userId, String measurementType, BigDecimal value, String unit, Instant recordedAt) {
        this.userId = userId;
        this.measurementType = measurementType;
        this.value = value;
        this.unit = unit;
        this.recordedAt = recordedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getMeasurementType() {
        return measurementType;
    }

    public BigDecimal getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}

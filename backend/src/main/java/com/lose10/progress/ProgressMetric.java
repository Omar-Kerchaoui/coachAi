package com.lose10.progress;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "progress_metrics")
public class ProgressMetric extends BaseEntity {

    @Column(name = "snapshot_id", nullable = false)
    private UUID snapshotId;

    @Column(name = "metric_key", nullable = false, length = 50)
    private String metricKey;

    @Column(name = "metric_value", nullable = false)
    private BigDecimal metricValue;

    protected ProgressMetric() {
    }

    public ProgressMetric(UUID snapshotId, String metricKey, BigDecimal metricValue) {
        this.snapshotId = snapshotId;
        this.metricKey = metricKey;
        this.metricValue = metricValue;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public String getMetricKey() {
        return metricKey;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }
}

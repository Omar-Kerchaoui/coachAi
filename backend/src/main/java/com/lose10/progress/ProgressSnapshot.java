package com.lose10.progress;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "progress_snapshots")
public class ProgressSnapshot extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "rolling_avg_weight_kg")
    private BigDecimal rollingAvgWeightKg;

    @Column(name = "trend_kg_per_week")
    private BigDecimal trendKgPerWeek;

    protected ProgressSnapshot() {
    }

    public ProgressSnapshot(UUID userId, LocalDate periodStart, LocalDate periodEnd, BigDecimal rollingAvgWeightKg, BigDecimal trendKgPerWeek) {
        this.userId = userId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.rollingAvgWeightKg = rollingAvgWeightKg;
        this.trendKgPerWeek = trendKgPerWeek;
    }

    public void update(BigDecimal rollingAvgWeightKg, BigDecimal trendKgPerWeek) {
        this.rollingAvgWeightKg = rollingAvgWeightKg;
        this.trendKgPerWeek = trendKgPerWeek;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getRollingAvgWeightKg() {
        return rollingAvgWeightKg;
    }

    public BigDecimal getTrendKgPerWeek() {
        return trendKgPerWeek;
    }
}

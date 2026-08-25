package com.lose10.progress.dto;

import com.lose10.progress.ProgressSnapshot;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProgressSnapshotResponse(LocalDate periodEnd, BigDecimal rollingAvgWeightKg, BigDecimal trendKgPerWeek) {
    public static ProgressSnapshotResponse from(ProgressSnapshot snapshot) {
        return new ProgressSnapshotResponse(snapshot.getPeriodEnd(), snapshot.getRollingAvgWeightKg(), snapshot.getTrendKgPerWeek());
    }
}

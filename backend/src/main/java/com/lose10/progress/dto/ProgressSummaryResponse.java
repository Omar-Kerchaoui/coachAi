package com.lose10.progress.dto;

import com.lose10.progress.ProgressPhase;
import com.lose10.weight.dto.WeightTrendResponse;

import java.math.BigDecimal;

public record ProgressSummaryResponse(
        BigDecimal startingWeightKg,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        BigDecimal weightLostKg,
        BigDecimal weightRemainingKg,
        int progressPercent,
        ProgressPhase phase,
        WeightTrendResponse.TrendDirection trendDirection
) {
}

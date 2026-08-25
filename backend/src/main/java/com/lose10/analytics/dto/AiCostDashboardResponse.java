package com.lose10.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AiCostDashboardResponse(
        BigDecimal totalCostUsd,
        long totalRequests,
        long distinctUsers,
        BigDecimal avgCostPerUserUsd,
        Map<String, BigDecimal> costByModel,
        Map<String, BigDecimal> costByRequestType,
        List<DailySpend> dailySpend
) {
    public record DailySpend(LocalDate date, BigDecimal costUsd, long requestCount) {
    }
}

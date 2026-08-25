package com.lose10.analytics;

import com.lose10.ai.AiRequestLog;
import com.lose10.ai.AiUsageQueryService;
import com.lose10.analytics.dto.AiCostDashboardResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates directly from {@link AiRequestLog} rows for the requested
 * window (docs/backlog L10-911) — no separate rollup table, unlike business
 * metrics, specifically so the numbers are guaranteed to reconcile exactly
 * against the raw table (the AC this epic calls out explicitly). AI request
 * volume is orders of magnitude smaller than, say, analytics_events, so an
 * in-memory aggregation over a bounded date range is cheap enough to run on
 * the dashboard request path.
 */
@Service
public class AiCostDashboardService {

    private final AiUsageQueryService aiUsageQueryService;

    public AiCostDashboardService(AiUsageQueryService aiUsageQueryService) {
        this.aiUsageQueryService = aiUsageQueryService;
    }

    public AiCostDashboardResponse computeFor(Instant from, Instant to) {
        List<AiRequestLog> logs = aiUsageQueryService.between(from, to);

        BigDecimal totalCost = logs.stream().map(AiRequestLog::getEstimatedCostUsd).reduce(BigDecimal.ZERO, BigDecimal::add);
        long distinctUsers = logs.stream().map(AiRequestLog::getUserId).filter(java.util.Objects::nonNull).distinct().count();
        BigDecimal avgCostPerUser = distinctUsers == 0
                ? BigDecimal.ZERO
                : totalCost.divide(BigDecimal.valueOf(distinctUsers), 6, RoundingMode.HALF_UP);

        Map<String, BigDecimal> costByModel = logs.stream()
                .collect(Collectors.groupingBy(AiRequestLog::getModel, Collectors.reducing(BigDecimal.ZERO, AiRequestLog::getEstimatedCostUsd, BigDecimal::add)));
        Map<String, BigDecimal> costByRequestType = logs.stream()
                .collect(Collectors.groupingBy(AiRequestLog::getRequestType, Collectors.reducing(BigDecimal.ZERO, AiRequestLog::getEstimatedCostUsd, BigDecimal::add)));

        Map<java.time.LocalDate, List<AiRequestLog>> byDay = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate()));
        List<AiCostDashboardResponse.DailySpend> dailySpend = byDay.entrySet().stream()
                .map(e -> new AiCostDashboardResponse.DailySpend(
                        e.getKey(),
                        e.getValue().stream().map(AiRequestLog::getEstimatedCostUsd).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()))
                .sorted(Comparator.comparing(AiCostDashboardResponse.DailySpend::date))
                .toList();

        return new AiCostDashboardResponse(totalCost, logs.size(), distinctUsers, avgCostPerUser, costByModel, costByRequestType, dailySpend);
    }
}

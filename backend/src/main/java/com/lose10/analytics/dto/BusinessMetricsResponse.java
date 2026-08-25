package com.lose10.analytics.dto;

import com.lose10.analytics.BusinessMetricsDaily;

import java.time.LocalDate;

public record BusinessMetricsResponse(
        LocalDate metricDate,
        long mrrCents,
        long arrCents,
        int activeSubscribers,
        int trialingSubscribers,
        int newSubscribers,
        int churnedSubscribers,
        int trialConversions,
        int dau,
        int wau,
        int stalledUsers,
        int atRiskUsers,
        int milestonesReached
) {
    public static BusinessMetricsResponse from(BusinessMetricsDaily m) {
        return new BusinessMetricsResponse(
                m.getMetricDate(), m.getMrrCents(), m.getArrCents(), m.getActiveSubscribers(), m.getTrialingSubscribers(),
                m.getNewSubscribers(), m.getChurnedSubscribers(), m.getTrialConversions(), m.getDau(), m.getWau(),
                m.getStalledUsers(), m.getAtRiskUsers(), m.getMilestonesReached()
        );
    }

    public static BusinessMetricsResponse empty(LocalDate date) {
        return new BusinessMetricsResponse(date, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}

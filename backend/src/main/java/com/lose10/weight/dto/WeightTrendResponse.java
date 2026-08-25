package com.lose10.weight.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code direction} is derived from the rolling average over the trailing
 * window, never from a single day-over-day delta (docs/DATABASE.md §PROGRESS
 * — "must not overreact to single-day fluctuation").
 */
public record WeightTrendResponse(List<TrendPoint> points, BigDecimal trendKgPerWeek, TrendDirection direction) {

    public enum TrendDirection {
        DOWN, FLAT, UP
    }
}

package com.lose10.weight;

import com.lose10.weight.dto.TrendPoint;
import com.lose10.weight.dto.WeightTrendResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Turns raw weight entries into a 7-day rolling-average trend. Deliberately
 * never reacts to a single day's reading in isolation — docs/DATABASE.md
 * §PROGRESS: "must not overreact to single-day fluctuation" — direction is
 * only reported DOWN/UP once the rolling-average trend crosses a small
 * threshold, so a single off-day inside a longer downward trend can't flip
 * the reported direction.
 */
@Component
public class WeightTrendCalculator {

    private static final int ROLLING_WINDOW_DAYS = 7;
    private static final double DIRECTION_THRESHOLD_KG_PER_WEEK = 0.05;

    public WeightTrendResponse calculate(List<WeightEntry> entries) {
        if (entries.isEmpty()) {
            return new WeightTrendResponse(List.of(), null, WeightTrendResponse.TrendDirection.FLAT);
        }

        SortedMap<LocalDate, Double> dailyAverages = dailyAverages(entries);
        List<LocalDate> dates = List.copyOf(dailyAverages.keySet());

        List<TrendPoint> points = dates.stream()
                .map(date -> new TrendPoint(date, round(rollingAverageAt(dailyAverages, date))))
                .toList();

        if (points.size() < 2) {
            return new WeightTrendResponse(points, null, WeightTrendResponse.TrendDirection.FLAT);
        }

        double latestRollingAvg = rollingAverageAt(dailyAverages, dates.get(dates.size() - 1));
        LocalDate weekAgoAnchor = dates.get(dates.size() - 1).minusDays(ROLLING_WINDOW_DAYS);
        LocalDate baselineDate = dates.stream()
                .filter(d -> !d.isAfter(weekAgoAnchor))
                .reduce((first, second) -> second) // latest date that is still <= weekAgoAnchor
                .orElse(dates.get(0));
        double baselineRollingAvg = rollingAverageAt(dailyAverages, baselineDate);

        BigDecimal trendPerWeek = round(latestRollingAvg - baselineRollingAvg);
        WeightTrendResponse.TrendDirection direction = classify(trendPerWeek.doubleValue());

        return new WeightTrendResponse(points, trendPerWeek, direction);
    }

    private double rollingAverageAt(SortedMap<LocalDate, Double> dailyAverages, LocalDate date) {
        LocalDate windowStart = date.minusDays(ROLLING_WINDOW_DAYS - 1L);
        List<Double> windowValues = dailyAverages.entrySet().stream()
                .filter(e -> !e.getKey().isBefore(windowStart) && !e.getKey().isAfter(date))
                .map(Map.Entry::getValue)
                .toList();
        return windowValues.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }

    private SortedMap<LocalDate, Double> dailyAverages(List<WeightEntry> entries) {
        Map<LocalDate, List<WeightEntry>> byDate = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getRecordedAt().atZone(ZoneOffset.UTC).toLocalDate()));
        SortedMap<LocalDate, Double> result = new TreeMap<>();
        byDate.forEach((date, dayEntries) -> {
            double avg = dayEntries.stream().mapToDouble(e -> e.getWeightKg().doubleValue()).average().orElseThrow();
            result.put(date, avg);
        });
        return result;
    }

    private WeightTrendResponse.TrendDirection classify(double trendKgPerWeek) {
        if (trendKgPerWeek <= -DIRECTION_THRESHOLD_KG_PER_WEEK) {
            return WeightTrendResponse.TrendDirection.DOWN;
        }
        if (trendKgPerWeek >= DIRECTION_THRESHOLD_KG_PER_WEEK) {
            return WeightTrendResponse.TrendDirection.UP;
        }
        return WeightTrendResponse.TrendDirection.FLAT;
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }
}

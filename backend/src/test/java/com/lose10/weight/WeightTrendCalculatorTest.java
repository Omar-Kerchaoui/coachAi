package com.lose10.weight;

import com.lose10.weight.dto.WeightTrendResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WeightTrendCalculatorTest {

    private final WeightTrendCalculator calculator = new WeightTrendCalculator();
    private final UUID userId = UUID.randomUUID();

    @Test
    void reportsDownwardTrendForSteadilyDecreasingWeight() {
        List<WeightEntry> entries = smoothlyDecreasingSeries(21, 90.0, 0.15, null);

        WeightTrendResponse result = calculator.calculate(entries);

        assertThat(result.direction()).isEqualTo(WeightTrendResponse.TrendDirection.DOWN);
        assertThat(result.trendKgPerWeek().doubleValue()).isNegative();
    }

    @Test
    void singleOffDaySpikeDoesNotFlipAnOtherwiseDownwardTrend() {
        // 21 days of a clear downward trend, with one anomalous +1.0kg spike
        // on day 15 — a plausible "ate a lot of salty food" blip. The
        // rolling-average trend must still read DOWN, not UP, because it's
        // one entry diluted across a 7-day window on top of weeks of decline.
        List<WeightEntry> entries = smoothlyDecreasingSeries(21, 90.0, 0.15, 15);

        WeightTrendResponse result = calculator.calculate(entries);

        assertThat(result.direction()).isEqualTo(WeightTrendResponse.TrendDirection.DOWN);
    }

    @Test
    void emptyHistoryReportsFlatWithNoTrend() {
        WeightTrendResponse result = calculator.calculate(List.of());

        assertThat(result.direction()).isEqualTo(WeightTrendResponse.TrendDirection.FLAT);
        assertThat(result.trendKgPerWeek()).isNull();
        assertThat(result.points()).isEmpty();
    }

    private List<WeightEntry> smoothlyDecreasingSeries(int days, double startWeight, double dailyDropKg, Integer spikeDayIndex) {
        List<WeightEntry> entries = new ArrayList<>();
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < days; i++) {
            double weight = startWeight - (dailyDropKg * i);
            if (spikeDayIndex != null && i == spikeDayIndex) {
                weight += 1.0;
            }
            LocalDate date = start.plusDays(i);
            entries.add(new WeightEntry(
                    userId,
                    BigDecimal.valueOf(weight).setScale(2, java.math.RoundingMode.HALF_UP),
                    date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                    WeightSource.USER_REPORTED
            ));
        }
        return entries;
    }
}

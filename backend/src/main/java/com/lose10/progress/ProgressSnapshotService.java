package com.lose10.progress;

import com.lose10.weight.WeightService;
import com.lose10.weight.dto.WeightTrendResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Split out from {@link ProgressSnapshotJob} specifically so
 * {@link #computeSnapshotFor} is invoked through a real Spring proxy rather
 * than a same-class self-invocation — @Transactional is silently ignored on
 * self-invoked calls, which would have made the "update existing snapshot"
 * branch never actually flush to the database.
 */
@Service
public class ProgressSnapshotService {

    private final WeightService weightService;
    private final ProgressSnapshotRepository snapshotRepository;

    public ProgressSnapshotService(WeightService weightService, ProgressSnapshotRepository snapshotRepository) {
        this.weightService = weightService;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public void computeSnapshotFor(UUID userId) {
        WeightTrendResponse trend = weightService.trend(userId);
        if (trend.points().isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.minusDays(6);
        var latestRollingAverage = trend.points().get(trend.points().size() - 1).rollingAverageKg();

        snapshotRepository.findByUserIdAndPeriodEnd(userId, today)
                .ifPresentOrElse(
                        existing -> existing.update(latestRollingAverage, trend.trendKgPerWeek()),
                        () -> snapshotRepository.save(new ProgressSnapshot(userId, periodStart, today, latestRollingAverage, trend.trendKgPerWeek()))
                );
    }
}

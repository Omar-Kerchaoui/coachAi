package com.lose10.progress;

import com.lose10.goal.Goal;
import com.lose10.goal.GoalService;
import com.lose10.progress.dto.ProgressSnapshotResponse;
import com.lose10.progress.dto.ProgressSummaryResponse;
import com.lose10.weight.WeightService;
import com.lose10.weight.dto.WeightEntryResponse;
import com.lose10.weight.dto.WeightTrendResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class ProgressService {

    private final GoalService goalService;
    private final WeightService weightService;
    private final ProgressSnapshotRepository snapshotRepository;

    public ProgressService(GoalService goalService, WeightService weightService, ProgressSnapshotRepository snapshotRepository) {
        this.goalService = goalService;
        this.weightService = weightService;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public ProgressSummaryResponse summary(UUID userId) {
        Goal goal = goalService.requireActiveGoal(userId);
        WeightEntryResponse latest = weightService.latest(userId).orElse(null);
        WeightTrendResponse trend = weightService.trend(userId);

        BigDecimal starting = goal.getStartingWeightKg();
        BigDecimal target = goal.getTargetWeightKg();
        BigDecimal current = latest != null ? latest.weightKg() : starting;

        BigDecimal totalToLose = starting.subtract(target);
        BigDecimal lost = starting.subtract(current);
        BigDecimal remaining = current.subtract(target);

        int progressPercent = totalToLose.signum() == 0
                ? 100
                : lost.multiply(BigDecimal.valueOf(100))
                        .divide(totalToLose, 0, RoundingMode.HALF_UP)
                        .max(BigDecimal.ZERO)
                        .min(BigDecimal.valueOf(100))
                        .intValue();

        ProgressPhase phase = phaseFor(progressPercent);

        return new ProgressSummaryResponse(starting, current, target, lost, remaining, progressPercent, phase, trend.direction());
    }

    @Transactional(readOnly = true)
    public List<ProgressSnapshotResponse> snapshots(UUID userId) {
        return snapshotRepository.findByUserIdOrderByPeriodEndDesc(userId).stream()
                .map(ProgressSnapshotResponse::from)
                .toList();
    }

    private ProgressPhase phaseFor(int progressPercent) {
        if (progressPercent >= 100) {
            return ProgressPhase.ACHIEVED;
        }
        if (progressPercent >= 75) {
            return ProgressPhase.FINAL;
        }
        if (progressPercent >= 25) {
            return ProgressPhase.MIDDLE;
        }
        return ProgressPhase.EARLY;
    }
}

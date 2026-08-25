package com.lose10.weight;

import com.lose10.analytics.AnalyticsEventType;
import com.lose10.analytics.AnalyticsRecorder;
import com.lose10.goal.GoalService;
import com.lose10.platform.error.ApiException;
import com.lose10.weight.dto.LogWeightRequest;
import com.lose10.weight.dto.WeightEntryResponse;
import com.lose10.weight.dto.WeightTrendResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WeightService {

    /**
     * A logged weight more than 25% different from the user's last entry is
     * almost always a unit mix-up or typo, not a real overnight change —
     * flagged for confirmation rather than silently accepted. Shared logic:
     * this same rule backs the AI's logWeight tool (docs/AI_ARCHITECTURE.md
     * §4) once that tool is implemented in Epic 6, so the check lives here
     * rather than being duplicated per call site.
     */
    private static final BigDecimal IMPLAUSIBLE_DELTA_RATIO = BigDecimal.valueOf(0.25);

    private final WeightEntryRepository repository;
    private final WeightTrendCalculator trendCalculator;
    private final GoalService goalService;
    private final AnalyticsRecorder analyticsRecorder;

    public WeightService(WeightEntryRepository repository, WeightTrendCalculator trendCalculator, GoalService goalService, AnalyticsRecorder analyticsRecorder) {
        this.repository = repository;
        this.trendCalculator = trendCalculator;
        this.goalService = goalService;
        this.analyticsRecorder = analyticsRecorder;
    }

    @Transactional
    public WeightEntryResponse logWeight(UUID userId, LogWeightRequest request) {
        repository.findFirstByUserIdOrderByRecordedAtDesc(userId).ifPresent(last -> {
            BigDecimal delta = request.weightKg().subtract(last.getWeightKg()).abs();
            BigDecimal ratio = delta.divide(last.getWeightKg(), 4, java.math.RoundingMode.HALF_UP);
            if (ratio.compareTo(IMPLAUSIBLE_DELTA_RATIO) > 0 && !request.confirmImplausible()) {
                throw ApiException.businessRule(
                        "This is a large change from your last logged weight (" + last.getWeightKg()
                                + "kg). Resubmit with confirmImplausible=true if this is correct.");
            }
        });

        Instant recordedAt = request.recordedAt() != null ? request.recordedAt() : Instant.now();
        WeightEntry entry = repository.save(new WeightEntry(userId, request.weightKg(), recordedAt, WeightSource.USER_REPORTED));
        analyticsRecorder.record(AnalyticsEventType.WEIGHT_LOGGED, userId);
        goalService.checkMilestones(userId, request.weightKg());
        return WeightEntryResponse.from(entry);
    }

    @Transactional(readOnly = true)
    public Page<WeightEntryResponse> history(UUID userId, Instant from, Instant to, Pageable pageable) {
        return repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(userId, from, to, pageable)
                .map(WeightEntryResponse::from);
    }

    @Transactional(readOnly = true)
    public WeightTrendResponse trend(UUID userId) {
        List<WeightEntry> recent = repository.findTop90ByUserIdOrderByRecordedAtDesc(userId);
        return trendCalculator.calculate(recent);
    }

    @Transactional(readOnly = true)
    public Optional<WeightEntryResponse> latest(UUID userId) {
        return repository.findFirstByUserIdOrderByRecordedAtDesc(userId).map(WeightEntryResponse::from);
    }
}

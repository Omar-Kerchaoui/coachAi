package com.lose10.analytics;

import com.lose10.goal.Goal;
import com.lose10.goal.GoalService;
import com.lose10.subscription.SubscriptionService;
import com.lose10.subscription.SubscriptionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Computes one {@link BusinessMetricsDaily} row (docs/backlog L10-910).
 * Called by {@link BusinessMetricsRollupJob} nightly; the dashboard
 * endpoint only ever reads the resulting table, never recomputes live.
 */
@Service
public class BusinessMetricsRollupService {

    private final GoalService goalService;
    private final SubscriptionService subscriptionService;
    private final AnalyticsEventRepository analyticsEventRepository;
    private final BusinessMetricsDailyRepository metricsRepository;
    private final EngagementClassifier engagementClassifier;

    public BusinessMetricsRollupService(
            GoalService goalService,
            SubscriptionService subscriptionService,
            AnalyticsEventRepository analyticsEventRepository,
            BusinessMetricsDailyRepository metricsRepository,
            EngagementClassifier engagementClassifier
    ) {
        this.goalService = goalService;
        this.subscriptionService = subscriptionService;
        this.analyticsEventRepository = analyticsEventRepository;
        this.metricsRepository = metricsRepository;
        this.engagementClassifier = engagementClassifier;
    }

    @Transactional
    public void computeFor(LocalDate date) {
        Instant dayStart = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant weekStart = date.minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();

        long mrrCents = subscriptionService.monthlyRecurringRevenueCents();
        long arrCents = mrrCents * 12;
        int activeSubscribers = (int) subscriptionService.countByStatus(SubscriptionStatus.ACTIVE);
        int trialingSubscribers = (int) subscriptionService.countByStatus(SubscriptionStatus.TRIALING);
        int newSubscribers = (int) analyticsEventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.SUBSCRIPTION_STARTED, dayStart, dayEnd);
        int churnedSubscribers = (int) analyticsEventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.SUBSCRIPTION_CANCELLED, dayStart, dayEnd);
        int trialConversions = countTrialConversions(dayStart, dayEnd);
        int dau = (int) analyticsEventRepository.countDistinctUsersByCreatedAtBetween(dayStart, dayEnd);
        int wau = (int) analyticsEventRepository.countDistinctUsersByCreatedAtBetween(weekStart, dayEnd);
        int milestonesReached = (int) analyticsEventRepository.countByEventTypeAndCreatedAtBetween(AnalyticsEventType.GOAL_MILESTONE_REACHED, dayStart, dayEnd);

        int stalledCount = 0;
        int atRiskCount = 0;
        for (Goal goal : goalService.listActiveGoals()) {
            EngagementStatus status = classify(goal.getUserId(), now);
            if (status == EngagementStatus.STALLED) {
                stalledCount++;
            } else if (status == EngagementStatus.AT_RISK) {
                atRiskCount++;
            }
        }
        final int stalled = stalledCount;
        final int atRisk = atRiskCount;

        metricsRepository.findByMetricDate(date).ifPresentOrElse(
                existing -> existing.update(mrrCents, arrCents, activeSubscribers, trialingSubscribers, newSubscribers,
                        churnedSubscribers, trialConversions, dau, wau, stalled, atRisk, milestonesReached),
                () -> metricsRepository.save(new BusinessMetricsDaily(date, mrrCents, arrCents, activeSubscribers,
                        trialingSubscribers, newSubscribers, churnedSubscribers, trialConversions, dau, wau, stalled, atRisk, milestonesReached))
        );
    }

    private EngagementStatus classify(UUID userId, Instant now) {
        Instant lastWeight = analyticsEventRepository.findFirstByUserIdAndEventTypeOrderByCreatedAtDesc(userId, AnalyticsEventType.WEIGHT_LOGGED)
                .map(e -> e.getCreatedAt()).orElse(null);
        Instant lastEngagement = analyticsEventRepository.findFirstByUserIdAndEventTypeOrderByCreatedAtDesc(userId, AnalyticsEventType.AI_MESSAGE_RECEIVED)
                .map(e -> e.getCreatedAt()).orElse(null);
        return engagementClassifier.classify(now, lastWeight, lastEngagement);
    }

    private int countTrialConversions(Instant dayStart, Instant dayEnd) {
        List<AnalyticsEvent> started = analyticsEventRepository.findByEventTypeAndCreatedAtBetween(AnalyticsEventType.SUBSCRIPTION_STARTED, dayStart, dayEnd);
        return (int) started.stream()
                .filter(e -> e.getMetadata() != null && Boolean.TRUE.equals(e.getMetadata().get("fromTrial")))
                .count();
    }
}

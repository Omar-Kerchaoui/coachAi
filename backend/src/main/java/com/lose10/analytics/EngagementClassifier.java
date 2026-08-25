package com.lose10.analytics;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Classifies a user with an active goal as ACTIVE, AT_RISK, or STALLED for
 * the business metrics dashboard (docs/backlog L10-910's "'Stalled'
 * heuristic documented alongside its implementation").
 *
 * <p><b>STALLED</b>: no weight logged in the last {@value #STALLED_DAYS}
 * days. Weight logging is the one signal every active user produces
 * regardless of how much they talk to the coach, so its absence for two
 * full weeks is the strongest available signal that someone has quietly
 * dropped off, not just gone quiet in chat.
 *
 * <p><b>AT_RISK</b>: still logging weight, but no WhatsApp engagement (no
 * inbound message) in the last {@value #AT_RISK_DAYS} days — worth a
 * proactive nudge before they reach STALLED.
 *
 * <p>Pure and deterministic by design (like {@code WeightTrendCalculator})
 * so it can be unit-tested against constructed timestamps without a
 * database.
 */
@Component
public class EngagementClassifier {

    private static final int STALLED_DAYS = 14;
    private static final int AT_RISK_DAYS = 7;

    public EngagementStatus classify(Instant now, Instant lastWeightLoggedAt, Instant lastEngagementAt) {
        if (olderThan(now, lastWeightLoggedAt, STALLED_DAYS)) {
            return EngagementStatus.STALLED;
        }
        if (olderThan(now, lastEngagementAt, AT_RISK_DAYS)) {
            return EngagementStatus.AT_RISK;
        }
        return EngagementStatus.ACTIVE;
    }

    private boolean olderThan(Instant now, Instant timestamp, int days) {
        return timestamp == null || Duration.between(timestamp, now).toDays() >= days;
    }
}

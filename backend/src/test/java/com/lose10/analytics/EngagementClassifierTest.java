package com.lose10.analytics;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EngagementClassifierTest {

    private final EngagementClassifier classifier = new EngagementClassifier();
    private final Instant now = Instant.parse("2026-08-25T00:00:00Z");

    @Test
    void activeWhenBothWeightAndEngagementAreRecent() {
        Instant lastWeight = now.minusSeconds(2 * 86_400L);
        Instant lastEngagement = now.minusSeconds(1 * 86_400L);

        assertThat(classifier.classify(now, lastWeight, lastEngagement)).isEqualTo(EngagementStatus.ACTIVE);
    }

    @Test
    void atRiskWhenWeightIsRecentButEngagementIsStale() {
        Instant lastWeight = now.minusSeconds(3 * 86_400L);
        Instant lastEngagement = now.minusSeconds(8 * 86_400L);

        assertThat(classifier.classify(now, lastWeight, lastEngagement)).isEqualTo(EngagementStatus.AT_RISK);
    }

    @Test
    void stalledWhenWeightItselfIsStaleEvenIfEngagementLooksRecent() {
        Instant lastWeight = now.minusSeconds(20 * 86_400L);
        Instant lastEngagement = now.minusSeconds(1 * 86_400L);

        assertThat(classifier.classify(now, lastWeight, lastEngagement)).isEqualTo(EngagementStatus.STALLED);
    }

    @Test
    void stalledWhenNoWeightHasEverBeenLogged() {
        assertThat(classifier.classify(now, null, now)).isEqualTo(EngagementStatus.STALLED);
    }

    @Test
    void atRiskWhenNoEngagementHasEverHappenedButWeightIsRecent() {
        Instant lastWeight = now.minusSeconds(86_400L);

        assertThat(classifier.classify(now, lastWeight, null)).isEqualTo(EngagementStatus.AT_RISK);
    }

    @Test
    void boundaryAtExactlyStalledThresholdCountsAsStalled() {
        Instant lastWeight = now.minusSeconds(14 * 86_400L);

        assertThat(classifier.classify(now, lastWeight, now)).isEqualTo(EngagementStatus.STALLED);
    }
}

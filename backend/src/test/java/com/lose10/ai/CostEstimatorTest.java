package com.lose10.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CostEstimatorTest {

    private final CostEstimator estimator = new CostEstimator();

    @Test
    void computesExpectedCostForKnownModelAndTokenCounts() {
        // claude-sonnet-5: $3/M input, $15/M output.
        // 1,000,000 input + 1,000,000 output tokens => $3 + $15 = $18.
        BigDecimal cost = estimator.estimate("claude-sonnet-5", 1_000_000, 1_000_000);

        assertThat(cost).isEqualByComparingTo("18.000000");
    }

    @Test
    void scalesLinearlyWithTokenCount() {
        BigDecimal costForThousand = estimator.estimate("claude-sonnet-5", 1_000, 0);
        // 1,000 / 1,000,000 * $3 = $0.003
        assertThat(costForThousand).isEqualByComparingTo("0.003000");
    }

    @Test
    void unrecognizedModelUsesFallbackPricingRatherThanZero() {
        BigDecimal cost = estimator.estimate("some-future-model-not-in-the-table", 1_000_000, 0);

        assertThat(cost).isGreaterThan(BigDecimal.ZERO);
    }
}

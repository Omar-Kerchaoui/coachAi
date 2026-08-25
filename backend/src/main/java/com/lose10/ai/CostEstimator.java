package com.lose10.ai;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * USD-per-million-token pricing, keyed by model ID. Deliberately a plain
 * hardcoded table rather than a config file — it's small, needs precise
 * review whenever it changes (never silently drift), and the fallback rate
 * makes an unrecognized model fail loud (in the cost dashboard) rather than
 * silently reporting $0.
 *
 * Update this table when Anthropic's published pricing changes.
 */
@Component
public class CostEstimator {

    private record Pricing(BigDecimal inputPerMillion, BigDecimal outputPerMillion) {
    }

    private static final Map<String, Pricing> PRICING = Map.of(
            "claude-sonnet-5", new Pricing(BigDecimal.valueOf(3.00), BigDecimal.valueOf(15.00)),
            "claude-haiku-4-5", new Pricing(BigDecimal.valueOf(0.80), BigDecimal.valueOf(4.00)),
            "claude-opus-5", new Pricing(BigDecimal.valueOf(15.00), BigDecimal.valueOf(75.00))
    );

    private static final Pricing FALLBACK = new Pricing(BigDecimal.valueOf(3.00), BigDecimal.valueOf(15.00));

    public BigDecimal estimate(String model, int inputTokens, int outputTokens) {
        Pricing pricing = PRICING.getOrDefault(model, FALLBACK);
        BigDecimal inputCost = pricing.inputPerMillion()
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        BigDecimal outputCost = pricing.outputPerMillion()
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }
}

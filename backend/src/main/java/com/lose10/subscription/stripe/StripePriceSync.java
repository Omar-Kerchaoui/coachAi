package com.lose10.subscription.stripe;

import com.lose10.subscription.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Overwrites the placeholder Stripe price ids seeded by V10__billing.sql
 * with the real ones once configured — keeps pricing entirely data-driven
 * (docs/backlog L10-802) rather than requiring a new migration every time a
 * Stripe product is (re)created in test vs. live mode. A no-op when the env
 * vars are unset, same graceful-degradation shape as the rest of the app's
 * external integrations.
 */
@Component
public class StripePriceSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StripePriceSync.class);

    private final PlanRepository planRepository;
    private final String monthlyPriceId;
    private final String annualPriceId;

    public StripePriceSync(
            PlanRepository planRepository,
            @Value("${lose10.stripe.price-id-monthly:}") String monthlyPriceId,
            @Value("${lose10.stripe.price-id-annual:}") String annualPriceId
    ) {
        this.planRepository = planRepository;
        this.monthlyPriceId = monthlyPriceId;
        this.annualPriceId = annualPriceId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        syncOne("Monthly", monthlyPriceId);
        syncOne("Annual", annualPriceId);
    }

    private void syncOne(String planName, String priceId) {
        if (priceId == null || priceId.isBlank()) {
            return;
        }
        planRepository.findByNameIgnoreCase(planName).ifPresentOrElse(
                plan -> {
                    if (!priceId.equals(plan.getStripePriceId())) {
                        plan.updateStripePriceId(priceId);
                        log.info("Synced {} plan to Stripe price {}", planName, priceId);
                    }
                },
                () -> log.warn("No '{}' plan row found to sync a Stripe price id onto", planName)
        );
    }
}

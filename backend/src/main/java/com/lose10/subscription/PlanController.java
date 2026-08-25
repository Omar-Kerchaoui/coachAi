package com.lose10.subscription;

import com.lose10.subscription.dto.PlanSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public (unauthenticated) — the pricing page needs to render plan options
 * before the visitor has an account.
 */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanController {

    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<PlanSummary> list() {
        return planRepository.findByActiveTrue().stream()
                .map(p -> new PlanSummary(p.getId(), p.getName(), p.getAmountCents(), p.getCurrency(), p.getBillingInterval(), p.getTrialDays()))
                .toList();
    }
}

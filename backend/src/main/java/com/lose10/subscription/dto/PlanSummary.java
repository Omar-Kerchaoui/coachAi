package com.lose10.subscription.dto;

import com.lose10.subscription.PlanInterval;

import java.util.UUID;

public record PlanSummary(UUID id, String name, int amountCents, String currency, PlanInterval billingInterval, int trialDays) {
}

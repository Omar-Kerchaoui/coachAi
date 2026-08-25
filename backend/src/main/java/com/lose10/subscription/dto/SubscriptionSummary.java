package com.lose10.subscription.dto;

import com.lose10.subscription.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionSummary(
        boolean hasSubscription,
        String planName,
        SubscriptionStatus status,
        Instant trialEnd,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd
) {
    public static SubscriptionSummary none() {
        return new SubscriptionSummary(false, null, null, null, null, false);
    }
}

package com.lose10.subscription;

/**
 * Mirrors Stripe's own Subscription.status values exactly (docs/DATABASE.md
 * SUBSCRIPTIONS domain — "status mirrors Stripe"), so a webhook payload's
 * status string maps directly via {@code valueOf} with no translation table
 * to drift out of sync.
 */
public enum SubscriptionStatus {
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    UNPAID,
    PAUSED;

    public boolean grantsCoachingAccess() {
        return this == TRIALING || this == ACTIVE;
    }
}

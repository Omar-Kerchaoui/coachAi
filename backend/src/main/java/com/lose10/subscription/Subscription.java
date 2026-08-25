package com.lose10.subscription;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Created at checkout-session time with {@code stripeSubscriptionId == null}
 * and {@code status == INCOMPLETE} — that's before Stripe's own Subscription
 * object exists, so the row's only job at that point is to remember which
 * Stripe customer this checkout belongs to. It only becomes usable for
 * coaching access once a webhook confirms TRIALING/ACTIVE
 * (docs/backlog L10-805 — never set optimistically from a client redirect).
 */
@Entity
@Table(name = "subscriptions")
public class Subscription extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "stripe_customer_id", nullable = false, length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 100)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.INCOMPLETE;

    @Column(name = "trial_end")
    private Instant trialEnd;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    protected Subscription() {
    }

    public Subscription(UUID userId, UUID planId, String stripeCustomerId) {
        this.userId = userId;
        this.planId = planId;
        this.stripeCustomerId = stripeCustomerId;
    }

    /**
     * Applies the full state Stripe reports for a subscription lifecycle
     * event — the single point where this row's status can change, always
     * from a webhook payload, never from client-supplied state.
     */
    public void applyStripeState(String stripeSubscriptionId, SubscriptionStatus status, Instant trialEnd, Instant currentPeriodEnd, boolean cancelAtPeriodEnd) {
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.status = status;
        this.trialEnd = trialEnd;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public void markCancelAtPeriodEnd() {
        this.cancelAtPeriodEnd = true;
    }

    /**
     * The placeholder row created at checkout time guesses a plan before
     * Stripe confirms anything; once a lifecycle webhook reports the actual
     * price on the subscription, this corrects it in case the guess was
     * stale (e.g. the customer had an older pending checkout for a
     * different plan).
     */
    public void reassignPlan(UUID planId) {
        this.planId = planId;
    }

    public boolean grantsCoachingAccess() {
        return status.grantsCoachingAccess();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getTrialEnd() {
        return trialEnd;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }
}

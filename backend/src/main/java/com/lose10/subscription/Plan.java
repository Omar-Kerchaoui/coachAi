package com.lose10.subscription;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "plans")
public class Plan extends MutableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "stripe_price_id", nullable = false, length = 100)
    private String stripePriceId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false, length = 10)
    private PlanInterval billingInterval;

    @Column(name = "trial_days", nullable = false)
    private int trialDays;

    @Column(nullable = false)
    private boolean active = true;

    protected Plan() {
    }

    /**
     * The migration seeds a placeholder Stripe price id; once a real one is
     * configured via env var, {@code StripePriceSync} overwrites it on
     * startup so the DB stays the single source of truth the rest of the
     * app reads from (docs/backlog L10-802 — "pricing is data-driven, not
     * hardcoded in application logic").
     */
    public void updateStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
    }

    public String getName() {
        return name;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public PlanInterval getBillingInterval() {
        return billingInterval;
    }

    public int getTrialDays() {
        return trialDays;
    }

    public boolean isActive() {
        return active;
    }
}

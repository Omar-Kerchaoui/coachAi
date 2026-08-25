package com.lose10.subscription;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "stripe_payment_intent_id", nullable = false, length = 100)
    private String stripePaymentIntentId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    protected Payment() {
    }

    public Payment(UUID subscriptionId, String stripePaymentIntentId, int amountCents, String currency, PaymentStatus status) {
        this.subscriptionId = subscriptionId;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }
}

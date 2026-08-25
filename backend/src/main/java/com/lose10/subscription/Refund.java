package com.lose10.subscription;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "refunds")
public class Refund extends BaseEntity {

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "stripe_refund_id", nullable = false, length = 100)
    private String stripeRefundId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(length = 255)
    private String reason;

    protected Refund() {
    }

    public Refund(UUID paymentId, String stripeRefundId, int amountCents, String reason) {
        this.paymentId = paymentId;
        this.stripeRefundId = stripeRefundId;
        this.amountCents = amountCents;
        this.reason = reason;
    }
}

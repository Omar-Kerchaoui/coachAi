package com.lose10.subscription;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
public class Coupon extends MutableEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "stripe_coupon_id", nullable = false, length = 100)
    private String stripeCouponId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private CouponDiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(nullable = false)
    private boolean active = true;

    protected Coupon() {
    }

    /**
     * Own-table validity check for a fast, clear client-facing rejection —
     * Stripe remains the authoritative check at checkout-session creation,
     * this only avoids sending an obviously-dead code to Stripe.
     */
    public boolean isCurrentlyValid() {
        if (!active) {
            return false;
        }
        Instant now = Instant.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || !now.isAfter(validUntil);
    }

    public String getCode() {
        return code;
    }

    public String getStripeCouponId() {
        return stripeCouponId;
    }

    public CouponDiscountType getDiscountType() {
        return discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }
}

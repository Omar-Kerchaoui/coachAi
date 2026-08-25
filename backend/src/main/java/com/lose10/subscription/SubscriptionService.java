package com.lose10.subscription;

import com.lose10.analytics.AnalyticsEventType;
import com.lose10.analytics.AnalyticsRecorder;
import com.lose10.auth.UserDirectory;
import com.lose10.auth.dto.UserSummary;
import com.lose10.platform.error.ApiException;
import com.lose10.subscription.dto.CheckoutSessionResponse;
import com.lose10.subscription.dto.PortalSessionResponse;
import com.lose10.subscription.dto.SubscriptionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the full subscription lifecycle: Checkout/Portal session creation
 * (outbound Stripe API calls) and applying webhook-reported state (inbound,
 * authoritative — docs/backlog L10-805). These two directions never race
 * for the same field: outbound calls only ever set
 * {@code stripe_customer_id} and read-only fields; every status/period
 * field is written exclusively from {@link #applySubscriptionLifecycleEvent}.
 */
@Service
public class SubscriptionService implements SubscriptionAccessChecker {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final CouponRepository couponRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final StripeProvider stripeProvider;
    private final UserDirectory userDirectory;
    private final AnalyticsRecorder analyticsRecorder;
    private final String successUrl;
    private final String cancelUrl;
    private final String portalReturnUrl;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            CouponRepository couponRepository,
            PaymentRepository paymentRepository,
            RefundRepository refundRepository,
            StripeProvider stripeProvider,
            UserDirectory userDirectory,
            AnalyticsRecorder analyticsRecorder,
            @Value("${lose10.stripe.checkout-success-url}") String successUrl,
            @Value("${lose10.stripe.checkout-cancel-url}") String cancelUrl,
            @Value("${lose10.stripe.portal-return-url}") String portalReturnUrl
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.couponRepository = couponRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.stripeProvider = stripeProvider;
        this.userDirectory = userDirectory;
        this.analyticsRecorder = analyticsRecorder;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.portalReturnUrl = portalReturnUrl;
    }

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(UUID userId, UUID planId, String couponCode) {
        Plan plan = planRepository.findById(planId)
                .filter(Plan::isActive)
                .orElseThrow(() -> ApiException.notFound("Plan not found"));

        String stripeCouponId = resolveCoupon(couponCode);
        String customerId = getOrCreateCustomerId(userId, plan);

        StripeProvider.Result<StripeProvider.CheckoutSession> result = stripeProvider.createCheckoutSession(
                userId, customerId, plan.getStripePriceId(), plan.getTrialDays(), stripeCouponId, successUrl, cancelUrl);
        if (!result.success()) {
            throw ApiException.businessRule("Failed to create checkout session: " + result.errorMessage());
        }
        return new CheckoutSessionResponse(result.value().url());
    }

    @Transactional(readOnly = true)
    public PortalSessionResponse createPortalSession(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> ApiException.notFound("No subscription found for this account"));
        StripeProvider.Result<String> result = stripeProvider.createPortalSession(subscription.getStripeCustomerId(), portalReturnUrl);
        if (!result.success()) {
            throw ApiException.businessRule("Failed to create portal session: " + result.errorMessage());
        }
        return new PortalSessionResponse(result.value());
    }

    @Transactional
    public void cancelAtPeriodEnd(UUID userId) {
        Subscription subscription = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(s -> s.getStripeSubscriptionId() != null)
                .orElseThrow(() -> ApiException.notFound("No active subscription to cancel"));
        StripeProvider.Result<Void> result = stripeProvider.cancelAtPeriodEnd(subscription.getStripeSubscriptionId());
        if (!result.success()) {
            throw ApiException.businessRule("Failed to cancel subscription: " + result.errorMessage());
        }
        // Safe to set directly here (unlike a checkout redirect): this is
        // the result of a synchronous, confirmed Stripe API response, not
        // an assumption about what the client did. The webhook that follows
        // will reconcile the same field idempotently.
        subscription.markCancelAtPeriodEnd();
    }

    @Transactional(readOnly = true)
    public SubscriptionSummary getMySubscription(UUID userId) {
        return subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(sub -> new SubscriptionSummary(
                        true,
                        planRepository.findById(sub.getPlanId()).map(Plan::getName).orElse(null),
                        sub.getStatus(),
                        sub.getTrialEnd(),
                        sub.getCurrentPeriodEnd(),
                        sub.isCancelAtPeriodEnd()))
                .orElseGet(SubscriptionSummary::none);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAccess(UUID userId) {
        return subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(Subscription::grantsCoachingAccess)
                .orElse(false);
    }

    /**
     * Annual plans are normalized to a monthly-equivalent contribution —
     * standard MRR convention (docs/backlog L10-910). Computed on demand
     * from live subscription+plan rows rather than cached, since the
     * subscriber count here is small enough that this is cheap; the nightly
     * rollup (docs/backlog L10-910's "not a live heavy-aggregation query on
     * the dashboard request path") is about the dashboard read path, not
     * this internal call.
     */
    @Transactional(readOnly = true)
    public long monthlyRecurringRevenueCents() {
        return subscriptionRepository.findByStatusIn(List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING)).stream()
                .mapToLong(sub -> planRepository.findById(sub.getPlanId())
                        .map(plan -> plan.getBillingInterval() == PlanInterval.YEAR ? plan.getAmountCents() / 12L : (long) plan.getAmountCents())
                        .orElse(0L))
                .sum();
    }

    @Transactional(readOnly = true)
    public long countByStatus(SubscriptionStatus status) {
        return subscriptionRepository.countByStatus(status);
    }

    /**
     * Called from {@code SubscriptionWebhookService} for
     * customer.subscription.created/updated/deleted — the only place
     * {@code subscriptions.status} is ever written (docs/backlog L10-805).
     * Looks up first by Stripe subscription id (the common case after the
     * first event), falling back to Stripe customer id for the very first
     * event that actually assigns the subscription id.
     */
    @Transactional
    public UUID applySubscriptionLifecycleEvent(
            String stripeCustomerId,
            String stripeSubscriptionId,
            String stripeStatus,
            Instant trialEnd,
            Instant currentPeriodEnd,
            boolean cancelAtPeriodEnd,
            String stripePriceId
    ) {
        Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .or(() -> subscriptionRepository.findFirstByStripeCustomerIdOrderByCreatedAtDesc(stripeCustomerId))
                .orElseThrow(() -> ApiException.notFound("No local subscription row for Stripe customer " + stripeCustomerId));

        SubscriptionStatus previousStatus = subscription.getStatus();
        SubscriptionStatus newStatus = mapStatus(stripeStatus);
        subscription.applyStripeState(stripeSubscriptionId, newStatus, trialEnd, currentPeriodEnd, cancelAtPeriodEnd);

        if (stripePriceId != null) {
            planRepository.findByStripePriceId(stripePriceId).ifPresent(plan -> subscription.reassignPlan(plan.getId()));
        }
        emitLifecycleAnalytics(subscription.getUserId(), previousStatus, newStatus);
        return subscription.getId();
    }

    private void emitLifecycleAnalytics(UUID userId, SubscriptionStatus previousStatus, SubscriptionStatus newStatus) {
        if (previousStatus == newStatus) {
            return;
        }
        if (newStatus == SubscriptionStatus.TRIALING) {
            analyticsRecorder.record(AnalyticsEventType.TRIAL_STARTED, userId);
        } else if (newStatus == SubscriptionStatus.ACTIVE) {
            analyticsRecorder.record(AnalyticsEventType.SUBSCRIPTION_STARTED, userId, Map.of("fromTrial", previousStatus == SubscriptionStatus.TRIALING));
        } else if (newStatus == SubscriptionStatus.CANCELED) {
            analyticsRecorder.record(AnalyticsEventType.SUBSCRIPTION_CANCELLED, userId);
        }
    }

    @Transactional
    public void recordPayment(String stripeSubscriptionId, String stripePaymentIntentId, int amountCents, String currency, PaymentStatus status) {
        if (paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId).isPresent()) {
            return;
        }
        Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElse(null);
        if (subscription == null) {
            log.warn("Received payment event for unknown Stripe subscription {}", stripeSubscriptionId);
            return;
        }
        paymentRepository.save(new Payment(subscription.getId(), stripePaymentIntentId, amountCents, currency, status));
    }

    @Transactional
    public void recordRefund(String stripePaymentIntentId, String stripeRefundId, int amountCents, String reason) {
        if (refundRepository.findByStripeRefundId(stripeRefundId).isPresent()) {
            return;
        }
        Optional<Payment> payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId);
        if (payment.isEmpty()) {
            log.warn("Received refund event for unknown payment intent {}", stripePaymentIntentId);
            return;
        }
        payment.get().markRefunded();
        refundRepository.save(new Refund(payment.get().getId(), stripeRefundId, amountCents, reason));
    }

    private String resolveCoupon(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(couponCode)
                .filter(Coupon::isCurrentlyValid)
                .orElseThrow(() -> ApiException.businessRule("Coupon code is invalid or expired"));
        return coupon.getStripeCouponId();
    }

    private String getOrCreateCustomerId(UUID userId, Plan plan) {
        Optional<Subscription> existing = subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
        if (existing.isPresent()) {
            return existing.get().getStripeCustomerId();
        }
        UserSummary user = userDirectory.getSummary(userId);
        StripeProvider.Result<String> customerResult = stripeProvider.createCustomer(userId, user.email());
        if (!customerResult.success()) {
            throw ApiException.businessRule("Failed to create Stripe customer: " + customerResult.errorMessage());
        }
        subscriptionRepository.save(new Subscription(userId, plan.getId(), customerResult.value()));
        return customerResult.value();
    }

    private SubscriptionStatus mapStatus(String stripeStatus) {
        try {
            return SubscriptionStatus.valueOf(stripeStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognized Stripe subscription status '{}', defaulting to INCOMPLETE", stripeStatus);
            return SubscriptionStatus.INCOMPLETE;
        }
    }
}

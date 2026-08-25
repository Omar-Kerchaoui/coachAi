package com.lose10.subscription;

import java.util.UUID;

/**
 * Abstraction over the Stripe REST API — same rationale as
 * {@code WhatsAppProvider}/{@code AIProvider}: an interface so the webhook/
 * event-processing logic can be tested without a real Stripe account, and a
 * single implementation class is the only thing allowed to talk to Stripe
 * directly.
 */
public interface StripeProvider {

    Result<String> createCustomer(UUID userId, String email);

    Result<CheckoutSession> createCheckoutSession(UUID userId, String customerId, String priceId, int trialDays, String couponId, String successUrl, String cancelUrl);

    Result<String> createPortalSession(String customerId, String returnUrl);

    Result<Void> cancelAtPeriodEnd(String stripeSubscriptionId);

    record CheckoutSession(String sessionId, String url) {
    }

    record Result<T>(boolean success, T value, String errorMessage) {
        public static <T> Result<T> ok(T value) {
            return new Result<>(true, value, null);
        }

        public static <T> Result<T> failure(String errorMessage) {
            return new Result<>(false, null, errorMessage);
        }

        public <R> Result<R> map(java.util.function.Function<T, R> mapper) {
            return success ? Result.ok(mapper.apply(value)) : Result.failure(errorMessage);
        }
    }
}

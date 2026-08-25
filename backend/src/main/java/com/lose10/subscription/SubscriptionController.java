package com.lose10.subscription;

import com.lose10.platform.security.CurrentUser;
import com.lose10.subscription.dto.CheckoutSessionRequest;
import com.lose10.subscription.dto.CheckoutSessionResponse;
import com.lose10.subscription.dto.PortalSessionResponse;
import com.lose10.subscription.dto.SubscriptionSummary;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public ResponseEntity<SubscriptionSummary> me() {
        return ResponseEntity.ok(subscriptionService.getMySubscription(CurrentUser.id()));
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<CheckoutSessionResponse> checkoutSession(@Valid @RequestBody CheckoutSessionRequest request) {
        return ResponseEntity.ok(subscriptionService.createCheckoutSession(CurrentUser.id(), request.planId(), request.couponCode()));
    }

    @PostMapping("/portal-session")
    public ResponseEntity<PortalSessionResponse> portalSession() {
        return ResponseEntity.ok(subscriptionService.createPortalSession(CurrentUser.id()));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel() {
        subscriptionService.cancelAtPeriodEnd(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}

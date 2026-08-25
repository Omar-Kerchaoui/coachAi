package com.lose10.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, UUID> {
    Optional<SubscriptionEvent> findByStripeEventId(String stripeEventId);
}

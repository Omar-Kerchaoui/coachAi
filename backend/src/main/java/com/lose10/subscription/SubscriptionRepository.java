package com.lose10.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findFirstByStripeCustomerIdOrderByCreatedAtDesc(String stripeCustomerId);

    List<Subscription> findByStatusIn(List<SubscriptionStatus> statuses);

    long countByStatus(SubscriptionStatus status);
}

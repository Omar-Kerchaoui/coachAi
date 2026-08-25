package com.lose10.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByActiveTrue();

    Optional<Plan> findByStripePriceId(String stripePriceId);

    Optional<Plan> findByNameIgnoreCase(String name);
}

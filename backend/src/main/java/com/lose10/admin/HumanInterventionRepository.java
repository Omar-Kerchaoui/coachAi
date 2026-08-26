package com.lose10.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HumanInterventionRepository extends JpaRepository<HumanIntervention, UUID> {
    Optional<HumanIntervention> findFirstByUserIdAndEndedAtIsNullOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}

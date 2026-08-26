package com.lose10.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {
    Optional<TrainingPlan> findByUserIdAndStatus(UUID userId, TrainingPlanStatus status);

    Optional<TrainingPlan> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);
}

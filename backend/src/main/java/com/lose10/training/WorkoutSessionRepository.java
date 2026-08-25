package com.lose10.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {
    List<WorkoutSession> findTop20ByUserIdOrderByCompletedAtDesc(UUID userId);
}

package com.lose10.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    Optional<Goal> findByUserIdAndStatus(UUID userId, GoalStatus status);

    Optional<Goal> findByIdAndUserId(UUID id, UUID userId);

    List<Goal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndStatus(UUID userId, GoalStatus status);

    List<Goal> findByStatus(GoalStatus status);

    void deleteByUserId(UUID userId);
}

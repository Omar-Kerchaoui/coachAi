package com.lose10.coaching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoachingStateRepository extends JpaRepository<CoachingState, UUID> {
    Optional<CoachingState> findByUserId(UUID userId);
}

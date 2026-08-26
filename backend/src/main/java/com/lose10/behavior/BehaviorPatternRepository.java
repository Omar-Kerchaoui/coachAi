package com.lose10.behavior;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BehaviorPatternRepository extends JpaRepository<BehaviorPattern, UUID> {
    Optional<BehaviorPattern> findByUserIdAndPatternKey(UUID userId, String patternKey);

    List<BehaviorPattern> findByUserIdAndStatus(UUID userId, String status);

    void deleteByUserId(UUID userId);
}

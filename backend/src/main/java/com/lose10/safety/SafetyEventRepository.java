package com.lose10.safety;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SafetyEventRepository extends JpaRepository<SafetyEvent, UUID> {
    List<SafetyEvent> findByResolvedFalseOrderByCreatedAtDesc();

    List<SafetyEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}

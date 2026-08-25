package com.lose10.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, UUID> {

    List<AiRequestLog> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AiRequestLog> findByUserId(UUID userId);

    List<AiRequestLog> findByCreatedAtBetween(Instant from, Instant to);
}

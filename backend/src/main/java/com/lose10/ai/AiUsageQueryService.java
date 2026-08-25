package com.lose10.ai;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only access to {@link AiRequestLog} for callers outside this module
 * (admin user detail, AI cost dashboard) — keeps
 * {@link AiRequestLogRepository} itself module-private in spirit even
 * though JPA repositories are technically public types
 * (docs/ARCHITECTURE.md §4).
 */
@Service
public class AiUsageQueryService {

    private final AiRequestLogRepository repository;

    public AiUsageQueryService(AiRequestLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AiRequestLog> recentForUser(UUID userId) {
        return repository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<AiRequestLog> forUser(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<AiRequestLog> between(Instant from, Instant to) {
        return repository.findByCreatedAtBetween(from, to);
    }
}

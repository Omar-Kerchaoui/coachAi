package com.lose10.platform.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * The single entry point every module uses to write an audit record.
 * Recording never throws — a logging failure must not break the action
 * being audited; failures are logged locally instead.
 *
 * <p>{@code REQUIRES_NEW} is deliberate, not decorative: a caller running
 * inside a {@code @Transactional(readOnly = true)} method (e.g. the admin
 * user-detail view, which audits {@code USER_DATA_VIEWED} while reading)
 * gets a Hibernate session flush-mode that never flushes pending writes —
 * without its own transaction, this save silently vanishes, joining the
 * caller's read-only transaction instead of getting a real one of its own.
 * Caught via a live test: the audit row never appeared despite no
 * exception being thrown. Same root cause class as the
 * {@code RefreshTokenFamilyRevoker} fix earlier in this project.
 *
 * <p>Both overloads carry the annotation, not just the 6-arg one: the 5-arg
 * overload calling the 6-arg one is a same-class self-invocation, which
 * bypasses Spring's AOP proxy entirely — the first attempt at this fix
 * annotated only the 6-arg method and the bug persisted for exactly that
 * reason, caught by re-testing after the "fix" instead of assuming it
 * worked.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, AuditLog.ActorType actorType, String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        try {
            repository.save(new AuditLog(actorId, actorType, action, targetType, targetId, metadata));
        } catch (Exception e) {
            log.error("Failed to write audit log [action={}, targetType={}, targetId={}]", action, targetType, targetId, e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, AuditLog.ActorType actorType, String action, String targetType, UUID targetId) {
        record(actorId, actorType, action, targetType, targetId, Map.of());
    }
}

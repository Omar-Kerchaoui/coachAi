package com.lose10.platform.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * The single entry point every module uses to write an audit record.
 * Recording never throws — a logging failure must not break the action
 * being audited; failures are logged locally instead.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(UUID actorId, AuditLog.ActorType actorType, String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        try {
            repository.save(new AuditLog(actorId, actorType, action, targetType, targetId, metadata));
        } catch (Exception e) {
            log.error("Failed to write audit log [action={}, targetType={}, targetId={}]", action, targetType, targetId, e);
        }
    }

    public void record(UUID actorId, AuditLog.ActorType actorType, String action, String targetType, UUID targetId) {
        record(actorId, actorType, action, targetType, targetId, Map.of());
    }
}

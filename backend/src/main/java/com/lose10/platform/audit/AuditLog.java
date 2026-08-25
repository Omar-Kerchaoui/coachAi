package com.lose10.platform.audit;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Append-only record of security-relevant actions (docs/SECURITY.md §7).
 * Never write raw sensitive payloads into {@code metadata} — only enough to
 * reconstruct who did what to which resource, when.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected AuditLog() {
    }

    public AuditLog(UUID actorId, ActorType actorType, String action, String targetType, UUID targetId, Map<String, Object> metadata) {
        this.actorId = actorId;
        this.actorType = actorType;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.metadata = metadata;
    }

    public UUID getActorId() {
        return actorId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public enum ActorType {
        USER, ADMIN, SYSTEM
    }
}

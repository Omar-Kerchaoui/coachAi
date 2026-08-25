package com.lose10.memory;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A single durable fact/preference/pattern the coach has learned about a
 * user (docs/MEMORY.md). Deliberately distinct from short-lived
 * conversational state — see MEMORY.md §3 for the extraction heuristic
 * that decides what becomes one of these.
 */
@Entity
@Table(name = "memories")
public class MemoryRecord extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemoryCategory category;

    @Column(nullable = false, length = 100)
    private String key;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemoryImportance importance = MemoryImportance.MEDIUM;

    @Column(nullable = false)
    private BigDecimal confidence;

    @Column(name = "source_message_id")
    private UUID sourceMessageId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    protected MemoryRecord() {
    }

    public MemoryRecord(UUID userId, MemoryCategory category, String key, String content,
                         MemoryImportance importance, BigDecimal confidence, UUID sourceMessageId) {
        this.userId = userId;
        this.category = category;
        this.key = key;
        this.content = content;
        this.importance = importance;
        this.confidence = confidence;
        this.sourceMessageId = sourceMessageId;
    }

    public void updateContent(String newContent, BigDecimal newConfidence) {
        this.content = newContent;
        this.confidence = newConfidence;
    }

    public void markUsed() {
        this.lastUsedAt = Instant.now();
    }

    public void expireNow() {
        this.expiresAt = Instant.now();
    }

    public boolean isActive() {
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    public UUID getUserId() {
        return userId;
    }

    public MemoryCategory getCategory() {
        return category;
    }

    public String getKey() {
        return key;
    }

    public String getContent() {
        return content;
    }

    public MemoryImportance getImportance() {
        return importance;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }
}

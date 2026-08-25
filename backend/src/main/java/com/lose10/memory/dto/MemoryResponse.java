package com.lose10.memory.dto;

import com.lose10.memory.MemoryCategory;
import com.lose10.memory.MemoryImportance;
import com.lose10.memory.MemoryRecord;

import java.time.Instant;
import java.util.UUID;

public record MemoryResponse(
        UUID id,
        MemoryCategory category,
        String key,
        String content,
        MemoryImportance importance,
        double confidence,
        Instant createdAt,
        Instant lastUsedAt
) {
    public static MemoryResponse from(MemoryRecord record) {
        return new MemoryResponse(
                record.getId(), record.getCategory(), record.getKey(), record.getContent(),
                record.getImportance(), record.getConfidence().doubleValue(),
                record.getCreatedAt(), record.getLastUsedAt()
        );
    }
}

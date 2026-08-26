package com.lose10.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRecordRepository extends JpaRepository<MemoryRecord, UUID> {
    Optional<MemoryRecord> findByUserIdAndCategoryAndKey(UUID userId, MemoryCategory category, String key);

    List<MemoryRecord> findByUserIdAndCategory(UUID userId, MemoryCategory category);

    @Query("SELECT m FROM MemoryRecord m WHERE m.userId = :userId AND (m.expiresAt IS NULL OR m.expiresAt > :now)")
    List<MemoryRecord> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    List<MemoryRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<MemoryRecord> findByIdAndUserId(UUID id, UUID userId);

    void deleteByUserId(UUID userId);
}

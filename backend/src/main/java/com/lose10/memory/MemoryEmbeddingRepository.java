package com.lose10.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MemoryEmbeddingRepository extends JpaRepository<MemoryEmbedding, UUID> {
    Optional<MemoryEmbedding> findByMemoryId(UUID memoryId);
}

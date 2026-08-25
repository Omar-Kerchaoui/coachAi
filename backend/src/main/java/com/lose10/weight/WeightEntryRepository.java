package com.lose10.weight;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeightEntryRepository extends JpaRepository<WeightEntry, UUID> {

    Page<WeightEntry> findByUserIdAndRecordedAtBetweenOrderByRecordedAtDesc(UUID userId, Instant from, Instant to, Pageable pageable);

    Optional<WeightEntry> findFirstByUserIdOrderByRecordedAtDesc(UUID userId);

    List<WeightEntry> findTop90ByUserIdOrderByRecordedAtDesc(UUID userId);

    Optional<WeightEntry> findByIdAndUserId(UUID id, UUID userId);
}

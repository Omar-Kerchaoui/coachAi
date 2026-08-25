package com.lose10.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgressSnapshotRepository extends JpaRepository<ProgressSnapshot, UUID> {
    Optional<ProgressSnapshot> findByUserIdAndPeriodEnd(UUID userId, LocalDate periodEnd);

    List<ProgressSnapshot> findByUserIdOrderByPeriodEndDesc(UUID userId);
}

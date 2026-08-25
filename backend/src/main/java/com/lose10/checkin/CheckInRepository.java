package com.lose10.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    List<CheckIn> findTop20ByUserIdOrderByScheduledForDesc(UUID userId);

    Optional<CheckIn> findByIdAndUserId(UUID id, UUID userId);
}

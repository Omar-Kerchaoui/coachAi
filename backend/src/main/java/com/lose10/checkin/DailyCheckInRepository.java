package com.lose10.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, UUID> {
    Optional<DailyCheckIn> findByUserIdAndCheckInDate(UUID userId, LocalDate date);

    void deleteByUserId(UUID userId);
}

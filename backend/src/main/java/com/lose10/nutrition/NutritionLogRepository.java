package com.lose10.nutrition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface NutritionLogRepository extends JpaRepository<NutritionLog, UUID> {
    Optional<NutritionLog> findByUserIdAndLogDate(UUID userId, LocalDate logDate);

    void deleteByUserId(UUID userId);
}

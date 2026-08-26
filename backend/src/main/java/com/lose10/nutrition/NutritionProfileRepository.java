package com.lose10.nutrition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NutritionProfileRepository extends JpaRepository<NutritionProfile, UUID> {
    Optional<NutritionProfile> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}

package com.lose10.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrainingPlanVersionRepository extends JpaRepository<TrainingPlanVersion, UUID> {
    List<TrainingPlanVersion> findByPlanIdOrderByVersionNoDesc(UUID planId);
}

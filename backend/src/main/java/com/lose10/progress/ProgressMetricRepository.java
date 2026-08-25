package com.lose10.progress;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProgressMetricRepository extends JpaRepository<ProgressMetric, UUID> {
}

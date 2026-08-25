package com.lose10.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMetricsDailyRepository extends JpaRepository<BusinessMetricsDaily, UUID> {
    Optional<BusinessMetricsDaily> findByMetricDate(LocalDate metricDate);

    Optional<BusinessMetricsDaily> findFirstByOrderByMetricDateDesc();
}

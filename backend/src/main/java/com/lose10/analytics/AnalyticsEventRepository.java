package com.lose10.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {

    long countByEventTypeAndCreatedAtBetween(AnalyticsEventType eventType, Instant from, Instant to);

    List<AnalyticsEvent> findByEventTypeAndCreatedAtBetween(AnalyticsEventType eventType, Instant from, Instant to);

    @Query("select count(distinct e.userId) from AnalyticsEvent e where e.createdAt between :from and :to and e.userId is not null")
    long countDistinctUsersByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(distinct e.userId) from AnalyticsEvent e where e.eventType = :eventType and e.createdAt between :from and :to and e.userId is not null")
    long countDistinctUsersByEventTypeAndCreatedAtBetween(@Param("eventType") AnalyticsEventType eventType, @Param("from") Instant from, @Param("to") Instant to);

    List<AnalyticsEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<AnalyticsEvent> findFirstByUserIdAndEventTypeOrderByCreatedAtDesc(UUID userId, AnalyticsEventType eventType);
}

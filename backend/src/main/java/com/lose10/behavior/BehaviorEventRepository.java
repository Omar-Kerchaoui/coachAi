package com.lose10.behavior;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BehaviorEventRepository extends JpaRepository<BehaviorEvent, UUID> {
    List<BehaviorEvent> findByUserIdAndEventTypeAndCreatedAtAfter(UUID userId, String eventType, Instant since);

    List<BehaviorEvent> findByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    @Query("SELECT DISTINCT e.userId FROM BehaviorEvent e WHERE e.createdAt > :since")
    List<UUID> findDistinctUserIdsWithEventsSince(@Param("since") Instant since);
}

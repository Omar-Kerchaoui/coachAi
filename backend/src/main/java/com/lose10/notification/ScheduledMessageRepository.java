package com.lose10.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledMessageRepository extends JpaRepository<ScheduledMessage, UUID> {
    long countByUserIdAndScheduledForBetween(UUID userId, Instant from, Instant to);

    List<ScheduledMessage> findByUserIdAndMessageTypeAndScheduledForBetween(UUID userId, MessageType type, Instant from, Instant to);
}

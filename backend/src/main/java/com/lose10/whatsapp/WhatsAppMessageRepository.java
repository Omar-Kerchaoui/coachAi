package com.lose10.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {
    Optional<WhatsAppMessage> findByWaMessageId(String waMessageId);

    List<WhatsAppMessage> findByCreatedAtBetween(Instant from, Instant to);
}

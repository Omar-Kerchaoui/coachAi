package com.lose10.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppWebhookEventRepository extends JpaRepository<WhatsAppWebhookEvent, UUID> {
    Optional<WhatsAppWebhookEvent> findByEventId(String eventId);
}

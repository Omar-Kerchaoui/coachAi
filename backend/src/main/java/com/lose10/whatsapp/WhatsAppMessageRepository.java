package com.lose10.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {
    Optional<WhatsAppMessage> findByWaMessageId(String waMessageId);

    List<WhatsAppMessage> findByCreatedAtBetween(Instant from, Instant to);

    /**
     * WhatsAppMessage has no direct user/account linkage — only
     * {@code message_id → messages(id) ON DELETE SET NULL} — so deleting a
     * user's ConversationMessage rows would just null this column, leaving
     * the row (and its wa_message_id/raw_payload) orphaned forever rather
     * than purged (docs/backlog L10-1004). Must be deleted explicitly,
     * before the referenced ConversationMessage rows are removed.
     */
    void deleteByMessageIdIn(List<UUID> messageIds);
}

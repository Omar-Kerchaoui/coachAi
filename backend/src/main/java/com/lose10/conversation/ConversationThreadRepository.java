package com.lose10.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationThreadRepository extends JpaRepository<ConversationThread, UUID> {
    Optional<ConversationThread> findFirstByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, String channel);
}

package com.lose10.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, UUID> {
}

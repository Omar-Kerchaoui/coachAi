package com.lose10.conversation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);

    @Query("select m.id from ConversationMessage m where m.threadId in :threadIds")
    List<UUID> findIdsByThreadIdIn(@Param("threadIds") List<UUID> threadIds);

    List<ConversationMessage> findByThreadIdInOrderByCreatedAtAsc(List<UUID> threadIds);

    java.util.Optional<ConversationMessage> findFirstByThreadIdAndDirectionOrderByCreatedAtDesc(UUID threadId, MessageDirection direction);
}

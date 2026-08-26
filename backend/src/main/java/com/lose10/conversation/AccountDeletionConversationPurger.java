package com.lose10.conversation;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges conversation threads on account deletion (docs/backlog L10-1004).
 * Messages/summaries cascade at the DB level from conversation_threads.
 * Runs after {@code AccountDeletionWhatsAppPurger} (see its Javadoc for
 * why the ordering matters).
 */
@Component
public class AccountDeletionConversationPurger {

    private final ConversationThreadRepository threadRepository;

    public AccountDeletionConversationPurger(ConversationThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }

    @EventListener
    @Order(2)
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        threadRepository.deleteByUserId(event.userId());
    }
}

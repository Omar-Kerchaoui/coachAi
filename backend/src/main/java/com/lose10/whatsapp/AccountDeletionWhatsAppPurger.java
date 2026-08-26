package com.lose10.whatsapp;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import com.lose10.conversation.ConversationMessageRepository;
import com.lose10.conversation.ConversationThread;
import com.lose10.conversation.ConversationThreadRepository;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Purges the WhatsApp account and any orphan-prone whatsapp_messages rows
 * on account deletion (docs/backlog L10-1004). Must run before
 * {@code AccountDeletionConversationPurger} — WhatsAppMessage has no direct
 * user linkage, only {@code message_id → messages(id) ON DELETE SET NULL},
 * so once the conversation messages are gone there'd be no way left to
 * find which whatsapp_messages rows belonged to this user.
 */
@Component
public class AccountDeletionWhatsAppPurger {

    private final WhatsAppAccountRepository accountRepository;
    private final WhatsAppMessageRepository messageRepository;
    private final ConversationThreadRepository threadRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    public AccountDeletionWhatsAppPurger(
            WhatsAppAccountRepository accountRepository,
            WhatsAppMessageRepository messageRepository,
            ConversationThreadRepository threadRepository,
            ConversationMessageRepository conversationMessageRepository
    ) {
        this.accountRepository = accountRepository;
        this.messageRepository = messageRepository;
        this.threadRepository = threadRepository;
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @EventListener
    @Order(1)
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        List<UUID> threadIds = threadRepository.findByUserId(event.userId()).stream().map(ConversationThread::getId).toList();
        if (!threadIds.isEmpty()) {
            List<UUID> messageIds = conversationMessageRepository.findIdsByThreadIdIn(threadIds);
            if (!messageIds.isEmpty()) {
                messageRepository.deleteByMessageIdIn(messageIds);
            }
        }
        accountRepository.deleteByUserId(event.userId());
    }
}

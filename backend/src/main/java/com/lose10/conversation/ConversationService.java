package com.lose10.conversation;

import com.lose10.conversation.event.MessageRecordedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Owns thread/message persistence around the orchestrator pipeline
 * (docs/ARCHITECTURE.md §5 — "Persist raw inbound message" /
 * "Persist outbound message"). Currently one open thread per user per
 * channel (docs/DATABASE.md CONVERSATIONS domain).
 */
@Service
public class ConversationService {

    private static final String DEFAULT_CHANNEL = "WHATSAPP";

    private final ConversationThreadRepository threadRepository;
    private final ConversationMessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ConversationService(ConversationThreadRepository threadRepository, ConversationMessageRepository messageRepository, ApplicationEventPublisher eventPublisher) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ConversationThread getOrCreateThread(UUID userId) {
        return threadRepository.findFirstByUserIdAndChannelOrderByCreatedAtDesc(userId, DEFAULT_CHANNEL)
                .orElseGet(() -> threadRepository.save(new ConversationThread(userId, DEFAULT_CHANNEL)));
    }

    @Transactional
    public ConversationMessage recordMessage(UUID userId, MessageDirection direction, MessageSender sender, String content) {
        ConversationThread thread = getOrCreateThread(userId);
        ConversationMessage message = messageRepository.save(new ConversationMessage(thread.getId(), direction, sender, content));
        eventPublisher.publishEvent(new MessageRecordedEvent(userId, message.getId(), sender, content));
        return message;
    }

    /**
     * Resolves the id of the most recently recorded inbound message for a
     * user — used by {@code WhatsAppWebhookService} to link the delivery
     * metadata row to the conversation message it corresponds to, after
     * the fact (the inbound text is recorded deeper in the pipeline, inside
     * {@code CoachingTurnService}, which doesn't hand the id back out).
     * Without this link, {@code whatsapp_messages} rows for inbound
     * messages were never associated with any user at all — found via the
     * account-deletion cascade leaving them as untouchable orphans
     * (docs/backlog L10-1004).
     */
    @Transactional(readOnly = true)
    public UUID mostRecentInboundMessageId(UUID userId) {
        ConversationThread thread = getOrCreateThread(userId);
        return messageRepository.findFirstByThreadIdAndDirectionOrderByCreatedAtDesc(thread.getId(), MessageDirection.INBOUND)
                .map(ConversationMessage::getId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public UUID userIdForThread(UUID threadId) {
        return threadRepository.findById(threadId)
                .map(ConversationThread::getUserId)
                .orElseThrow(() -> com.lose10.platform.error.ApiException.notFound("Conversation thread not found"));
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> recentMessages(UUID userId, int limit) {
        ConversationThread thread = getOrCreateThread(userId);
        List<ConversationMessage> messages = messageRepository.findByThreadIdOrderByCreatedAtDesc(thread.getId(), PageRequest.of(0, limit));
        return messages.reversed();
    }

    /**
     * Full conversation history across every thread the user has ever had
     * (any channel), for the data export (docs/backlog L10-1003) — unlike
     * {@link #recentMessages}, not bounded to one recent window.
     */
    @Transactional(readOnly = true)
    public List<ConversationMessage> allMessages(UUID userId) {
        List<UUID> threadIds = threadRepository.findByUserId(userId).stream().map(ConversationThread::getId).toList();
        return threadIds.isEmpty() ? List.of() : messageRepository.findByThreadIdInOrderByCreatedAtAsc(threadIds);
    }
}

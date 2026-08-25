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

    @Transactional(readOnly = true)
    public List<ConversationMessage> recentMessages(UUID userId, int limit) {
        ConversationThread thread = getOrCreateThread(userId);
        List<ConversationMessage> messages = messageRepository.findByThreadIdOrderByCreatedAtDesc(thread.getId(), PageRequest.of(0, limit));
        return messages.reversed();
    }
}

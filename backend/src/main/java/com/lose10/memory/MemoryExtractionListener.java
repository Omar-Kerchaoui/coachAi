package com.lose10.memory;

import com.lose10.conversation.MessageSender;
import com.lose10.conversation.event.MessageRecordedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Wires {@link MessageRecordedEvent} to {@link MemoryExtractor}, running
 * off the request thread (docs/backlog L10-507) so extraction latency
 * never affects the coaching reply itself. Only user-authored messages are
 * ever extraction candidates — the coach's own replies aren't.
 */
@Component
public class MemoryExtractionListener {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractionListener.class);

    private final MemoryExtractor memoryExtractor;

    public MemoryExtractionListener(MemoryExtractor memoryExtractor) {
        this.memoryExtractor = memoryExtractor;
    }

    @Async("memoryExtractionExecutor")
    @EventListener
    public void onMessageRecorded(MessageRecordedEvent event) {
        if (event.sender() != MessageSender.USER) {
            return;
        }
        try {
            memoryExtractor.extractFrom(event.userId(), event.content(), event.messageId());
        } catch (Exception e) {
            log.error("Memory extraction failed for message {}", event.messageId(), e);
        }
    }
}

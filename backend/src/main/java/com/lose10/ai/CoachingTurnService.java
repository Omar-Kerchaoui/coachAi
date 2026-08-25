package com.lose10.ai;

import com.lose10.ai.orchestrator.AIOrchestrator;
import com.lose10.ai.orchestrator.OrchestratorResult;
import com.lose10.conversation.ConversationService;
import com.lose10.conversation.MessageDirection;
import com.lose10.conversation.MessageSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Ties conversation persistence to the orchestrator, matching the pipeline
 * shape in docs/ARCHITECTURE.md §5 end to end: persist inbound → run the
 * orchestrator → persist outbound. This is the single entry point any
 * inbound-message channel calls — currently only the test endpoint
 * ({@code AiTestController}); Epic 7's WhatsApp webhook handler will call
 * this same method rather than duplicating the wiring.
 */
@Service
public class CoachingTurnService {

    private final ConversationService conversationService;
    private final AIOrchestrator orchestrator;

    public CoachingTurnService(ConversationService conversationService, AIOrchestrator orchestrator) {
        this.conversationService = conversationService;
        this.orchestrator = orchestrator;
    }

    public String handleInboundMessage(UUID userId, String inboundText) {
        conversationService.recordMessage(userId, MessageDirection.INBOUND, MessageSender.USER, inboundText);

        OrchestratorResult result = orchestrator.handle(userId, inboundText);

        // COACHING_NOT_AUTOMATIC means a human has taken over or coaching is
        // paused (docs/SECURITY.md §11) — nothing gets sent, so nothing gets
        // recorded as an outbound message either.
        if (result.status() == OrchestratorResult.Status.COACHING_NOT_AUTOMATIC) {
            return null;
        }

        MessageSender outboundSender = result.status() == OrchestratorResult.Status.SUCCESS ? MessageSender.AI : MessageSender.SYSTEM;
        conversationService.recordMessage(userId, MessageDirection.OUTBOUND, outboundSender, result.message());

        return result.message();
    }
}

package com.lose10.ai;

import com.lose10.ai.orchestrator.AIOrchestrator;
import com.lose10.ai.orchestrator.OrchestratorResult;
import com.lose10.analytics.AnalyticsEventType;
import com.lose10.analytics.AnalyticsRecorder;
import com.lose10.conversation.ConversationMessage;
import com.lose10.conversation.ConversationService;
import com.lose10.conversation.MessageDirection;
import com.lose10.conversation.MessageSender;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Ties conversation persistence to the orchestrator, matching the pipeline
 * shape in docs/ARCHITECTURE.md §5 end to end: persist inbound → run the
 * orchestrator → persist outbound. This is the single entry point any
 * inbound-message channel calls — the test endpoint ({@code AiTestController})
 * and the WhatsApp webhook handler ({@code WhatsAppWebhookService}) both call
 * this rather than duplicating the wiring.
 *
 * <p>Returns the persisted {@link ConversationMessage}, not just its text —
 * this is the single point where the outbound message is recorded. A
 * caller that needs to actually deliver it (e.g. over WhatsApp) must not
 * record it again itself; it should only attach delivery metadata to the
 * message returned here. Recording it twice was a real bug caught via a
 * live webhook test: the outbound text ended up persisted once under
 * MessageSender.SYSTEM (from here) and again under MessageSender.AI (from
 * the caller redundantly calling recordMessage a second time).
 */
@Service
public class CoachingTurnService {

    private final ConversationService conversationService;
    private final AIOrchestrator orchestrator;
    private final AnalyticsRecorder analyticsRecorder;

    public CoachingTurnService(ConversationService conversationService, AIOrchestrator orchestrator, AnalyticsRecorder analyticsRecorder) {
        this.conversationService = conversationService;
        this.orchestrator = orchestrator;
        this.analyticsRecorder = analyticsRecorder;
    }

    /**
     * @return the persisted outbound message, or empty if coaching is
     * PAUSED/HUMAN_ACTIVE (docs/SECURITY.md §11) — nothing is sent or
     * recorded in that case.
     */
    public Optional<ConversationMessage> handleInboundMessage(UUID userId, String inboundText) {
        conversationService.recordMessage(userId, MessageDirection.INBOUND, MessageSender.USER, inboundText);
        analyticsRecorder.record(AnalyticsEventType.AI_MESSAGE_RECEIVED, userId);

        OrchestratorResult result = orchestrator.handle(userId, inboundText);

        if (result.status() == OrchestratorResult.Status.COACHING_NOT_AUTOMATIC) {
            return Optional.empty();
        }

        MessageSender outboundSender = result.status() == OrchestratorResult.Status.SUCCESS ? MessageSender.AI : MessageSender.SYSTEM;
        ConversationMessage outbound = conversationService.recordMessage(userId, MessageDirection.OUTBOUND, outboundSender, result.message());
        if (outboundSender == MessageSender.AI) {
            analyticsRecorder.record(AnalyticsEventType.AI_MESSAGE_SENT, userId);
        }
        return Optional.of(outbound);
    }
}

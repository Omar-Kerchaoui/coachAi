package com.lose10.checkin;

import com.lose10.ai.orchestrator.AIOrchestrator;
import com.lose10.ai.orchestrator.OrchestratorResult;
import com.lose10.conversation.ConversationService;
import com.lose10.conversation.MessageDirection;
import com.lose10.conversation.MessageSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Split out from {@link CheckInService} specifically to avoid a circular
 * dependency: this class depends on {@code AIOrchestrator}, which itself
 * depends (via {@code ContextBuilder}) on {@code CheckInService} for the
 * "recent check-in" context section. Keeping the AI dependency out of
 * {@code CheckInService} breaks the cycle.
 */
@Service
public class WeeklyReviewGenerator {

    private static final String WEEKLY_REVIEW_PROMPT =
            "Please give me a short weekly review summarizing my progress this week and encouragement for next week.";

    private final AIOrchestrator orchestrator;
    private final ConversationService conversationService;
    private final CheckInService checkInService;

    public WeeklyReviewGenerator(AIOrchestrator orchestrator, ConversationService conversationService, CheckInService checkInService) {
        this.orchestrator = orchestrator;
        this.conversationService = conversationService;
        this.checkInService = checkInService;
    }

    /**
     * Routed through the same orchestrator pipeline as any coaching reply
     * (safety screen, context assembly, response validation) rather than a
     * raw unvalidated prompt call (docs/backlog L10-610). The synthetic
     * trigger prompt is never persisted as though the user said it — only
     * the coach's resulting review text is recorded, as an outbound
     * message, which is accurate: the review really is something the
     * coach says.
     */
    @Transactional
    public WeeklyReview generate(UUID userId, LocalDate weekStart) {
        OrchestratorResult result = orchestrator.handle(userId, WEEKLY_REVIEW_PROMPT);
        conversationService.recordMessage(userId, MessageDirection.OUTBOUND, MessageSender.AI, result.message());
        return checkInService.saveWeeklyReview(userId, weekStart, result.message());
    }
}

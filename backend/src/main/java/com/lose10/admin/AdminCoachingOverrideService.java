package com.lose10.admin;

import com.lose10.coaching.CoachingMode;
import com.lose10.coaching.CoachingState;
import com.lose10.coaching.CoachingStateService;
import com.lose10.conversation.ConversationMessage;
import com.lose10.conversation.ConversationService;
import com.lose10.conversation.MessageDirection;
import com.lose10.conversation.MessageSender;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.platform.error.ApiException;
import com.lose10.whatsapp.WhatsAppMessageSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The human-coach override path (docs/backlog L10-907): flipping
 * {@link CoachingMode} and sending a message as the human coach instead of
 * the AI. Setting HUMAN_ACTIVE is checked at the same orchestrator entry
 * gate documented on {@code CoachingStateService.allowsAutomaticAiResponse}
 * — this class only decides *when* that flag flips, never bypasses it.
 */
@Service
public class AdminCoachingOverrideService {

    private final CoachingStateService coachingStateService;
    private final HumanInterventionRepository interventionRepository;
    private final ConversationService conversationService;
    private final WhatsAppMessageSender whatsAppMessageSender;
    private final AuditLogService auditLogService;

    public AdminCoachingOverrideService(
            CoachingStateService coachingStateService,
            HumanInterventionRepository interventionRepository,
            ConversationService conversationService,
            WhatsAppMessageSender whatsAppMessageSender,
            AuditLogService auditLogService
    ) {
        this.coachingStateService = coachingStateService;
        this.interventionRepository = interventionRepository;
        this.conversationService = conversationService;
        this.whatsAppMessageSender = whatsAppMessageSender;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CoachingState setMode(UUID userId, UUID adminId, CoachingMode targetMode, String reason) {
        CoachingState state = switch (targetMode) {
            case PAUSED -> coachingStateService.pause(userId, reason, adminId);
            case AI_ACTIVE -> {
                endOpenIntervention(userId);
                yield coachingStateService.resume(userId, adminId);
            }
            case HUMAN_ACTIVE -> {
                startInterventionIfNeeded(userId, adminId, reason);
                yield coachingStateService.setHumanActive(userId, adminId);
            }
        };
        return state;
    }

    /**
     * Only sendable while HUMAN_ACTIVE (docs/backlog L10-907 AC) — an admin
     * message sent while the AI is still active would look like two coaches
     * talking over each other.
     */
    @Transactional
    public ConversationMessage sendManualMessage(UUID userId, UUID adminId, String text) {
        CoachingState state = coachingStateService.getOrCreate(userId);
        if (state.getMode() != CoachingMode.HUMAN_ACTIVE) {
            throw ApiException.businessRule("Coaching must be in HUMAN_ACTIVE mode to send a manual message");
        }
        ConversationMessage message = conversationService.recordMessage(userId, MessageDirection.OUTBOUND, MessageSender.ADMIN, text);
        whatsAppMessageSender.send(userId, message.getId(), text);
        auditLogService.record(adminId, AuditLog.ActorType.ADMIN, "ADMIN_MESSAGE_SENT", "User", userId);
        return message;
    }

    private void startInterventionIfNeeded(UUID userId, UUID adminId, String reason) {
        if (interventionRepository.findFirstByUserIdAndEndedAtIsNullOrderByCreatedAtDesc(userId).isEmpty()) {
            interventionRepository.save(new HumanIntervention(userId, adminId, reason));
        }
    }

    private void endOpenIntervention(UUID userId) {
        interventionRepository.findFirstByUserIdAndEndedAtIsNullOrderByCreatedAtDesc(userId).ifPresent(HumanIntervention::end);
    }
}

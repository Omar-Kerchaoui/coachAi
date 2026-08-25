package com.lose10.admin;

import com.lose10.admin.dto.AdminUserDetailResponse;
import com.lose10.ai.AiRequestLog;
import com.lose10.ai.AiUsageQueryService;
import com.lose10.auth.UserDirectory;
import com.lose10.checkin.CheckIn;
import com.lose10.checkin.CheckInService;
import com.lose10.coaching.CoachingStateService;
import com.lose10.conversation.ConversationMessage;
import com.lose10.conversation.ConversationService;
import com.lose10.goal.GoalService;
import com.lose10.goal.dto.GoalResponse;
import com.lose10.memory.MemoryService;
import com.lose10.memory.dto.MemoryResponse;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.safety.SafetyEvent;
import com.lose10.safety.SafetyEventService;
import com.lose10.subscription.SubscriptionService;
import com.lose10.subscription.dto.SubscriptionSummary;
import com.lose10.weight.WeightService;
import com.lose10.weight.dto.WeightEntryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Assembles the full cross-domain admin view for one user (docs/backlog
 * L10-906) purely by calling each owning module's own public service —
 * this class has no repositories of its own for other modules' data.
 */
@Service
public class AdminUserDetailService {

    private final UserDirectory userDirectory;
    private final ConversationService conversationService;
    private final CoachingStateService coachingStateService;
    private final GoalService goalService;
    private final WeightService weightService;
    private final SubscriptionService subscriptionService;
    private final CheckInService checkInService;
    private final MemoryService memoryService;
    private final SafetyEventService safetyEventService;
    private final AiUsageQueryService aiUsageQueryService;
    private final AdminNoteRepository adminNoteRepository;
    private final AuditLogService auditLogService;

    public AdminUserDetailService(
            UserDirectory userDirectory,
            ConversationService conversationService,
            CoachingStateService coachingStateService,
            GoalService goalService,
            WeightService weightService,
            SubscriptionService subscriptionService,
            CheckInService checkInService,
            MemoryService memoryService,
            SafetyEventService safetyEventService,
            AiUsageQueryService aiUsageQueryService,
            AdminNoteRepository adminNoteRepository,
            AuditLogService auditLogService
    ) {
        this.userDirectory = userDirectory;
        this.conversationService = conversationService;
        this.coachingStateService = coachingStateService;
        this.goalService = goalService;
        this.weightService = weightService;
        this.subscriptionService = subscriptionService;
        this.checkInService = checkInService;
        this.memoryService = memoryService;
        this.safetyEventService = safetyEventService;
        this.aiUsageQueryService = aiUsageQueryService;
        this.adminNoteRepository = adminNoteRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getDetail(UUID userId, UUID adminId) {
        auditLogService.record(adminId, AuditLog.ActorType.ADMIN, "USER_DATA_VIEWED", "User", userId);

        List<GoalResponse> goals = goalService.listGoals(userId);
        List<WeightEntryResponse> recentWeights = weightService.history(userId, java.time.Instant.EPOCH, java.time.Instant.now(), PageRequest.of(0, 20)).getContent();
        WeightEntryResponse latestWeight = weightService.latest(userId).orElse(null);
        SubscriptionSummary subscription = subscriptionService.getMySubscription(userId);
        var coachingState = coachingStateService.getOrCreate(userId);

        List<AdminUserDetailResponse.CheckInSummary> checkIns = checkInService.recent(userId).stream()
                .map(this::toCheckInSummary).toList();
        List<AdminUserDetailResponse.MemorySummary> memories = memoryService.listOwn(userId).stream()
                .map(this::toMemorySummary).toList();
        List<AdminUserDetailResponse.SafetyEventSummary> safetyEvents = safetyEventService.forUser(userId).stream()
                .map(this::toSafetySummary).toList();
        List<AdminUserDetailResponse.NoteSummary> notes = adminNoteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new AdminUserDetailResponse.NoteSummary(n.getId(), n.getAdminId(), n.getNote(), n.getCreatedAt()))
                .toList();

        List<AiRequestLog> aiUsage = aiUsageQueryService.forUser(userId);
        BigDecimal totalCost = aiUsage.stream().map(AiRequestLog::getEstimatedCostUsd).reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID threadId = conversationService.getOrCreateThread(userId).getId();
        List<AdminUserDetailResponse.ConversationMessageSummary> recentMessages = conversationService.recentMessages(userId, 30).stream()
                .map(this::toMessageSummary).toList();

        return new AdminUserDetailResponse(
                userDirectory.getDetail(userId),
                threadId,
                coachingState.getMode(),
                coachingState.getPausedReason(),
                goals,
                latestWeight,
                recentWeights,
                subscription,
                checkIns,
                memories,
                safetyEvents,
                notes,
                new AdminUserDetailResponse.AiUsageSummary(aiUsage.size(), totalCost),
                recentMessages
        );
    }

    private AdminUserDetailResponse.CheckInSummary toCheckInSummary(CheckIn c) {
        return new AdminUserDetailResponse.CheckInSummary(c.getId(), c.getCheckInType().name(), c.isCompleted(), c.getScheduledFor(), c.getCompletedAt());
    }

    private AdminUserDetailResponse.MemorySummary toMemorySummary(MemoryResponse m) {
        return new AdminUserDetailResponse.MemorySummary(m.id(), m.category().name(), m.key(), m.content(), m.importance().name());
    }

    private AdminUserDetailResponse.SafetyEventSummary toSafetySummary(SafetyEvent e) {
        return new AdminUserDetailResponse.SafetyEventSummary(e.getId(), e.getCategory().name(), e.getSeverity().name(), e.isResolved(), e.getCreatedAt());
    }

    private AdminUserDetailResponse.ConversationMessageSummary toMessageSummary(ConversationMessage m) {
        return new AdminUserDetailResponse.ConversationMessageSummary(m.getId(), m.getDirection().name(), m.getSender().name(), m.getContent(), m.getCreatedAt());
    }
}

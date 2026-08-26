package com.lose10.profile;

import com.lose10.checkin.CheckInService;
import com.lose10.conversation.ConversationService;
import com.lose10.goal.GoalService;
import com.lose10.memory.MemoryService;
import com.lose10.nutrition.NutritionService;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.profile.dto.DataExportResponse;
import com.lose10.subscription.SubscriptionService;
import com.lose10.training.TrainingService;
import com.lose10.weight.WeightService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Assembles the full personal-data export (docs/backlog L10-1003) purely
 * by calling each owning module's own public service — same aggregation
 * pattern as {@code AdminUserDetailService}, just self-service rather than
 * admin-facing, and covering the full history rather than a recent window.
 */
@Service
public class DataExportService {

    private final ProfileService profileService;
    private final GoalService goalService;
    private final WeightService weightService;
    private final TrainingService trainingService;
    private final NutritionService nutritionService;
    private final CheckInService checkInService;
    private final MemoryService memoryService;
    private final ConversationService conversationService;
    private final SubscriptionService subscriptionService;
    private final AuditLogService auditLogService;

    public DataExportService(
            ProfileService profileService,
            GoalService goalService,
            WeightService weightService,
            TrainingService trainingService,
            NutritionService nutritionService,
            CheckInService checkInService,
            MemoryService memoryService,
            ConversationService conversationService,
            SubscriptionService subscriptionService,
            AuditLogService auditLogService
    ) {
        this.profileService = profileService;
        this.goalService = goalService;
        this.weightService = weightService;
        this.trainingService = trainingService;
        this.nutritionService = nutritionService;
        this.checkInService = checkInService;
        this.memoryService = memoryService;
        this.conversationService = conversationService;
        this.subscriptionService = subscriptionService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public DataExportResponse export(UUID userId) {
        var consents = profileService.listConsents(userId).stream()
                .map(c -> new DataExportResponse.ConsentRecord(c.getConsentType(), c.getVersion(), c.getGrantedAt(), c.getRevokedAt()))
                .toList();

        var weightEntries = weightService.history(userId, Instant.EPOCH, Instant.now(), PageRequest.of(0, 5000)).getContent();

        var workoutSessions = trainingService.recentSessions(userId).stream()
                .map(s -> new DataExportResponse.WorkoutSessionRecord(s.getId(), s.getWorkoutId(), s.getCompletedAt(), s.getPerceivedEffort(), s.getNotes()))
                .toList();

        var nutritionProfile = nutritionService.getOrCreateProfile(userId);
        var nutritionRecord = new DataExportResponse.NutritionProfileRecord(
                nutritionProfile.getDietaryRestrictions(), nutritionProfile.getDislikedFoods(),
                nutritionProfile.getTargetCalories(), nutritionProfile.getTargetProteinG());

        var checkIns = checkInService.recent(userId).stream()
                .map(c -> new DataExportResponse.CheckInRecord(c.getId(), c.getCheckInType().name(), c.isCompleted(), c.getScheduledFor(), c.getCompletedAt()))
                .toList();

        var memories = memoryService.listOwn(userId).stream()
                .map(m -> new DataExportResponse.MemoryRecordExport(
                        m.id(), m.category().name(), m.key(), m.content(), m.importance().name(),
                        java.math.BigDecimal.valueOf(m.confidence()), m.createdAt()))
                .toList();

        var messages = conversationService.allMessages(userId).stream()
                .map(m -> new DataExportResponse.ConversationMessageRecord(m.getId(), m.getDirection().name(), m.getSender().name(), m.getContent(), m.getCreatedAt()))
                .toList();

        DataExportResponse response = new DataExportResponse(
                Instant.now(),
                profileService.getProfile(userId),
                consents,
                goalService.listGoals(userId),
                weightEntries,
                workoutSessions,
                nutritionRecord,
                checkIns,
                memories,
                messages,
                subscriptionService.getMySubscription(userId)
        );

        auditLogService.record(userId, AuditLog.ActorType.USER, "USER_DATA_EXPORTED", "User", userId);
        return response;
    }
}

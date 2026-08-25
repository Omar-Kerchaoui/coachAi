package com.lose10.admin.dto;

import com.lose10.auth.dto.UserDetail;
import com.lose10.coaching.CoachingMode;
import com.lose10.goal.dto.GoalResponse;
import com.lose10.subscription.dto.SubscriptionSummary;
import com.lose10.weight.dto.WeightEntryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The full cross-domain view for one user (docs/backlog L10-906, product
 * spec §43). Assembled from each owning module's own public service — never
 * from a cross-module repository join — so this stays a read model, not a
 * new coupling point.
 */
public record AdminUserDetailResponse(
        UserDetail user,
        UUID conversationThreadId,
        CoachingMode coachingMode,
        String coachingPausedReason,
        List<GoalResponse> goals,
        WeightEntryResponse latestWeight,
        List<WeightEntryResponse> recentWeights,
        SubscriptionSummary subscription,
        List<CheckInSummary> recentCheckIns,
        List<MemorySummary> memories,
        List<SafetyEventSummary> safetyEvents,
        List<NoteSummary> notes,
        AiUsageSummary aiUsage,
        List<ConversationMessageSummary> recentMessages
) {
    public record CheckInSummary(UUID id, String type, boolean completed, Instant scheduledFor, Instant completedAt) {
    }

    public record MemorySummary(UUID id, String category, String key, String content, String importance) {
    }

    public record SafetyEventSummary(UUID id, String category, String severity, boolean resolved, Instant createdAt) {
    }

    public record NoteSummary(UUID id, UUID adminId, String note, Instant createdAt) {
    }

    public record AiUsageSummary(long requestCount, BigDecimal totalCostUsd) {
    }

    public record ConversationMessageSummary(UUID id, String direction, String sender, String content, Instant createdAt) {
    }
}

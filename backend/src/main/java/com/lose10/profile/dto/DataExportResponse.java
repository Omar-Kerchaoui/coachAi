package com.lose10.profile.dto;

import com.lose10.goal.dto.GoalResponse;
import com.lose10.subscription.dto.SubscriptionSummary;
import com.lose10.weight.dto.WeightEntryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The full downloadable export (docs/backlog L10-1003): profile, goals,
 * weight, workouts, nutrition, conversations, memories, subscription, and
 * consents — every domain the product spec names. Returned as a direct
 * authenticated JSON download rather than an async S3-presigned-URL job:
 * there's no S3 in this environment, and at this data volume a synchronous
 * response is simpler and just as correct — the point of this feature is
 * "the user can get all their data out," not the delivery mechanism.
 */
public record DataExportResponse(
        Instant exportedAt,
        ProfileResponse profile,
        List<ConsentRecord> consents,
        List<GoalResponse> goals,
        List<WeightEntryResponse> weightEntries,
        List<WorkoutSessionRecord> workoutSessions,
        NutritionProfileRecord nutritionProfile,
        List<CheckInRecord> checkIns,
        List<MemoryRecordExport> memories,
        List<ConversationMessageRecord> conversationMessages,
        SubscriptionSummary subscription
) {
    public record ConsentRecord(String consentType, String version, Instant grantedAt, Instant revokedAt) {
    }

    public record WorkoutSessionRecord(UUID id, UUID workoutId, Instant completedAt, Integer perceivedEffort, String notes) {
    }

    public record NutritionProfileRecord(List<String> dietaryRestrictions, List<String> dislikedFoods, Integer targetCalories, Integer targetProteinG) {
    }

    public record CheckInRecord(UUID id, String type, boolean completed, Instant scheduledFor, Instant completedAt) {
    }

    public record MemoryRecordExport(UUID id, String category, String key, String content, String importance, BigDecimal confidence, Instant createdAt) {
    }

    public record ConversationMessageRecord(UUID id, String direction, String sender, String content, Instant createdAt) {
    }
}

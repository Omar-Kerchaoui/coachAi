package com.lose10.ai.orchestrator;

import com.lose10.behavior.BehaviorPattern;
import com.lose10.behavior.BehaviorPatternRepository;
import com.lose10.checkin.CheckIn;
import com.lose10.checkin.CheckInService;
import com.lose10.conversation.ConversationMessage;
import com.lose10.conversation.ConversationService;
import com.lose10.conversation.MessageSender;
import com.lose10.goal.Goal;
import com.lose10.goal.GoalService;
import com.lose10.memory.MemoryRecord;
import com.lose10.memory.MemoryRetriever;
import com.lose10.nutrition.NutritionProfile;
import com.lose10.nutrition.NutritionService;
import com.lose10.platform.error.ApiException;
import com.lose10.profile.ProfileService;
import com.lose10.profile.dto.ProfileResponse;
import com.lose10.training.TrainingService;
import com.lose10.training.dto.WorkoutResponse;
import com.lose10.weight.WeightService;
import com.lose10.weight.dto.WeightEntryResponse;
import com.lose10.weight.dto.WeightTrendResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Assembles the compact, structured context block sent to Claude alongside
 * every coaching turn (docs/AI_ARCHITECTURE.md §3). Each section is
 * independently sourced from a domain service — never a raw dump of the
 * database or full conversation history.
 */
@Component
public class ContextBuilder {

    private final ProfileService profileService;
    private final GoalService goalService;
    private final WeightService weightService;
    private final MemoryRetriever memoryRetriever;
    private final BehaviorPatternRepository behaviorPatternRepository;
    private final ConversationService conversationService;
    private final TrainingService trainingService;
    private final NutritionService nutritionService;
    private final CheckInService checkInService;
    private final int maxRecentMessages;

    public ContextBuilder(
            ProfileService profileService,
            GoalService goalService,
            WeightService weightService,
            MemoryRetriever memoryRetriever,
            BehaviorPatternRepository behaviorPatternRepository,
            ConversationService conversationService,
            TrainingService trainingService,
            NutritionService nutritionService,
            CheckInService checkInService,
            @Value("${lose10.ai.context.max-recent-messages}") int maxRecentMessages
    ) {
        this.profileService = profileService;
        this.goalService = goalService;
        this.weightService = weightService;
        this.memoryRetriever = memoryRetriever;
        this.behaviorPatternRepository = behaviorPatternRepository;
        this.conversationService = conversationService;
        this.trainingService = trainingService;
        this.nutritionService = nutritionService;
        this.checkInService = checkInService;
        this.maxRecentMessages = maxRecentMessages;
    }

    public String build(UUID userId, String inboundMessage, Intent intent) {
        StringBuilder sb = new StringBuilder();
        appendProfile(sb, userId);
        appendGoalAndProgress(sb, userId);
        appendTodayWorkout(sb, userId);
        appendNutritionPreferences(sb, userId);
        appendRecentCheckIn(sb, userId);
        appendRelevantMemories(sb, userId, inboundMessage);
        appendBehaviorPatterns(sb, userId);
        appendRecentConversation(sb, userId);
        return sb.toString();
    }

    private void appendProfile(StringBuilder sb, UUID userId) {
        ProfileResponse profile = profileService.getProfile(userId);
        sb.append("USER PROFILE\n")
                .append("timezone: ").append(profile.timezone()).append('\n')
                .append("coach style: ").append(profile.coachStyle()).append('\n')
                .append("units: ").append(profile.units()).append("\n\n");
    }

    private void appendGoalAndProgress(StringBuilder sb, UUID userId) {
        Optional<Goal> goal = requireActiveGoalOrEmpty(userId);
        if (goal.isEmpty()) {
            sb.append("GOAL\nNo active goal yet — user has not completed onboarding.\n\n");
            return;
        }

        Goal activeGoal = goal.get();
        sb.append("GOAL\n")
                .append("starting weight: ").append(activeGoal.getStartingWeightKg()).append("kg\n")
                .append("target weight: ").append(activeGoal.getTargetWeightKg()).append("kg\n")
                .append("weight to lose: ").append(activeGoal.weightToLoseKg()).append("kg\n\n");

        Optional<WeightEntryResponse> latest = weightService.latest(userId);
        WeightTrendResponse trend = weightService.trend(userId);
        sb.append("CURRENT WEIGHT + TREND\n");
        latest.ifPresentOrElse(
                w -> sb.append("latest logged weight: ").append(w.weightKg()).append("kg\n"),
                () -> sb.append("no weight logged yet\n"));
        sb.append("trend direction: ").append(trend.direction()).append('\n');
        if (trend.trendKgPerWeek() != null) {
            sb.append("trend: ").append(trend.trendKgPerWeek()).append("kg/week\n");
        }
        sb.append('\n');
    }

    private void appendTodayWorkout(StringBuilder sb, UUID userId) {
        try {
            Optional<WorkoutResponse> workout = trainingService.todayWorkout(userId, LocalDate.now().getDayOfWeek());
            sb.append("TODAY'S WORKOUT\n");
            workout.ifPresentOrElse(
                    w -> sb.append(w.workoutType()).append(": ").append(w.exercises().size()).append(" exercises\n"),
                    () -> sb.append("No workout scheduled today\n"));
            sb.append('\n');
        } catch (ApiException e) {
            // No active training plan yet — omit the section rather than error.
        }
    }

    private void appendNutritionPreferences(StringBuilder sb, UUID userId) {
        NutritionProfile profile = nutritionService.getOrCreateProfile(userId);
        boolean hasRestrictions = profile.getDietaryRestrictions() != null && !profile.getDietaryRestrictions().isEmpty();
        boolean hasDislikes = profile.getDislikedFoods() != null && !profile.getDislikedFoods().isEmpty();
        if (!hasRestrictions && !hasDislikes) {
            return;
        }
        sb.append("NUTRITION PREFERENCES\n");
        if (hasRestrictions) {
            sb.append("dietary restrictions: ").append(String.join(", ", profile.getDietaryRestrictions())).append('\n');
        }
        if (hasDislikes) {
            sb.append("dislikes: ").append(String.join(", ", profile.getDislikedFoods())).append('\n');
        }
        sb.append('\n');
    }

    private void appendRecentCheckIn(StringBuilder sb, UUID userId) {
        List<CheckIn> recent = checkInService.recent(userId);
        Optional<CheckIn> lastCompleted = recent.stream().filter(CheckIn::isCompleted).findFirst();
        if (lastCompleted.isEmpty()) {
            return;
        }
        CheckIn checkIn = lastCompleted.get();
        sb.append("LAST CHECK-IN (").append(checkIn.getCheckInType()).append(")\n")
                .append(checkIn.getResponses()).append("\n\n");
    }

    private void appendRelevantMemories(StringBuilder sb, UUID userId, String inboundMessage) {
        List<MemoryRecord> memories = memoryRetriever.retrieve(userId, inboundMessage, null);
        if (memories.isEmpty()) {
            return;
        }
        sb.append("RELEVANT MEMORIES (weave in naturally — never say \"according to my memory\")\n");
        for (MemoryRecord memory : memories) {
            sb.append("- [").append(memory.getCategory()).append("] ").append(memory.getContent()).append('\n');
        }
        sb.append('\n');
    }

    private void appendBehaviorPatterns(StringBuilder sb, UUID userId) {
        List<BehaviorPattern> patterns = behaviorPatternRepository.findByUserIdAndStatus(userId, "ACTIVE");
        if (patterns.isEmpty()) {
            return;
        }
        sb.append("BEHAVIOR PATTERNS\n");
        for (BehaviorPattern pattern : patterns) {
            sb.append("- ").append(pattern.getDescription()).append('\n');
        }
        sb.append('\n');
    }

    private void appendRecentConversation(StringBuilder sb, UUID userId) {
        List<ConversationMessage> recent = conversationService.recentMessages(userId, maxRecentMessages);
        if (recent.isEmpty()) {
            return;
        }
        sb.append("RECENT CONVERSATION\n");
        for (ConversationMessage message : recent) {
            String speaker = message.getSender() == MessageSender.USER ? "User" : "Coach";
            sb.append(speaker).append(": ").append(message.getContent()).append('\n');
        }
        sb.append('\n');
    }

    private Optional<Goal> requireActiveGoalOrEmpty(UUID userId) {
        try {
            return Optional.of(goalService.requireActiveGoal(userId));
        } catch (ApiException e) {
            return Optional.empty();
        }
    }
}

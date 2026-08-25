package com.lose10.ai.tools;

import com.lose10.coaching.CoachingMode;
import com.lose10.coaching.CoachingStateService;
import com.lose10.platform.error.ApiException;
import com.lose10.training.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Restricted while a human coach has taken over (docs/AI_ARCHITECTURE.md
 * §4) — casual conversation shouldn't be able to silently rewrite a plan
 * a human is actively managing.
 */
@Component
public class UpdateTrainingPlanTool implements AiTool {

    private final TrainingService trainingService;
    private final CoachingStateService coachingStateService;

    public UpdateTrainingPlanTool(TrainingService trainingService, CoachingStateService coachingStateService) {
        this.trainingService = trainingService;
        this.coachingStateService = coachingStateService;
    }

    @Override
    public String name() {
        return "updateTrainingPlan";
    }

    @Override
    public String description() {
        return "Creates a new, lighter version of the user's training plan (fewer days, shorter sessions) — use "
                + "when the user reports being consistently unable to keep up, not for a single bad day.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("reason", Map.of("type", "string", "description", "why the plan is being adjusted")),
                "required", List.of("reason")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        if (coachingStateService.getOrCreate(userId).getMode() == CoachingMode.HUMAN_ACTIVE) {
            return ToolResult.error("A human coach is currently managing this user's plan");
        }
        try {
            trainingService.reduceLoad(userId, (String) arguments.get("reason"));
            return ToolResult.ok("plan updated");
        } catch (ApiException e) {
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Failed to update training plan");
        }
    }
}

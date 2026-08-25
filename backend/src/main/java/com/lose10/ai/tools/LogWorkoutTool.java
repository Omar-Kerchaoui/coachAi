package com.lose10.ai.tools;

import com.lose10.training.TrainingService;
import com.lose10.training.dto.LogWorkoutSessionRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class LogWorkoutTool implements AiTool {

    private final TrainingService trainingService;

    public LogWorkoutTool(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @Override
    public String name() {
        return "logWorkout";
    }

    @Override
    public String description() {
        return "Logs a completed workout session for the user's today's workout.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "workoutId", Map.of("type", "string", "description", "the id of the workout being completed"),
                        "perceivedEffort", Map.of("type", "integer", "description", "1-10"),
                        "notes", Map.of("type", "string")
                ),
                "required", List.of("workoutId")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            UUID workoutId = UUID.fromString((String) arguments.get("workoutId"));
            Integer effort = arguments.get("perceivedEffort") instanceof Number n ? n.intValue() : null;
            String notes = (String) arguments.get("notes");
            trainingService.logSession(userId, new LogWorkoutSessionRequest(workoutId, effort, notes));
            return ToolResult.ok("logged");
        } catch (Exception e) {
            return ToolResult.error("Failed to log workout — was the workout id valid?");
        }
    }
}

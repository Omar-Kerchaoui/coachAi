package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.training.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetTrainingPlanTool implements AiTool {

    private final TrainingService trainingService;
    private final ObjectMapper objectMapper;

    public GetTrainingPlanTool(TrainingService trainingService, ObjectMapper objectMapper) {
        this.trainingService = trainingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getTrainingPlan";
    }

    @Override
    public String description() {
        return "Returns the user's full active training plan (all scheduled workouts for the week).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(trainingService.currentPlanWorkouts(userId)));
        } catch (Exception e) {
            return ToolResult.error(e.getMessage() != null ? e.getMessage() : "Failed to read training plan");
        }
    }
}

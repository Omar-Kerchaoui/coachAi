package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.training.TrainingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetRecentWorkoutsTool implements AiTool {

    private final TrainingService trainingService;
    private final ObjectMapper objectMapper;

    public GetRecentWorkoutsTool(TrainingService trainingService, ObjectMapper objectMapper) {
        this.trainingService = trainingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getRecentWorkouts";
    }

    @Override
    public String description() {
        return "Returns the user's recently completed workout sessions.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(trainingService.recentSessions(userId)));
        } catch (Exception e) {
            return ToolResult.error("Failed to read recent workouts");
        }
    }
}

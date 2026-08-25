package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.training.TrainingService;
import com.lose10.training.dto.WorkoutResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class GetTodayWorkoutTool implements AiTool {

    private final TrainingService trainingService;
    private final ObjectMapper objectMapper;

    public GetTodayWorkoutTool(TrainingService trainingService, ObjectMapper objectMapper) {
        this.trainingService = trainingService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getTodayWorkout";
    }

    @Override
    public String description() {
        return "Returns today's scheduled workout, if there is one, from the user's active training plan.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            Optional<WorkoutResponse> workout = trainingService.todayWorkout(userId, LocalDate.now().getDayOfWeek());
            return ToolResult.ok(workout.map(this::toJson).orElse("{\"message\":\"No workout scheduled today\"}"));
        } catch (Exception e) {
            return ToolResult.error(e.getMessage() != null ? e.getMessage() : "Failed to read today's workout");
        }
    }

    private String toJson(WorkoutResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{}";
        }
    }
}

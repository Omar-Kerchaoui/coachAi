package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.goal.GoalService;
import com.lose10.goal.dto.GoalResponse;
import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetGoalTool implements AiTool {

    private final GoalService goalService;
    private final ObjectMapper objectMapper;

    public GetGoalTool(GoalService goalService, ObjectMapper objectMapper) {
        this.goalService = goalService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getGoal";
    }

    @Override
    public String description() {
        return "Returns the user's current active weight-loss goal: starting weight, target weight, and weight remaining to lose. Returns an error if the user has no active goal yet.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            GoalResponse goal = GoalResponse.from(goalService.requireActiveGoal(userId));
            return ToolResult.ok(objectMapper.writeValueAsString(goal));
        } catch (ApiException e) {
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Failed to read goal");
        }
    }
}

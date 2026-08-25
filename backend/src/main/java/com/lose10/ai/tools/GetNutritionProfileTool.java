package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.nutrition.NutritionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetNutritionProfileTool implements AiTool {

    private final NutritionService nutritionService;
    private final ObjectMapper objectMapper;

    public GetNutritionProfileTool(NutritionService nutritionService, ObjectMapper objectMapper) {
        this.nutritionService = nutritionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getNutritionProfile";
    }

    @Override
    public String description() {
        return "Returns the user's nutrition preferences: dietary restrictions, disliked foods, calorie/protein targets.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(nutritionService.getOrCreateProfile(userId)));
        } catch (Exception e) {
            return ToolResult.error("Failed to read nutrition profile");
        }
    }
}

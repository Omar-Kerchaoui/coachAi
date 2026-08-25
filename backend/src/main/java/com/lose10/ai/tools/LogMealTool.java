package com.lose10.ai.tools;

import com.lose10.nutrition.NutritionService;
import com.lose10.nutrition.dto.LogMealRequest;
import com.lose10.nutrition.dto.MealItemResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class LogMealTool implements AiTool {

    private final NutritionService nutritionService;

    public LogMealTool(NutritionService nutritionService) {
        this.nutritionService = nutritionService;
    }

    @Override
    public String name() {
        return "logMeal";
    }

    @Override
    public String description() {
        return "Logs a meal from a free-text description (e.g. 'eggs and toast'). Calories/protein are AI-estimated — no food database lookup needed.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "description", Map.of("type", "string"),
                        "mealType", Map.of("type", "string", "enum", List.of("BREAKFAST", "LUNCH", "DINNER", "SNACK"))
                ),
                "required", List.of("description")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            String description = (String) arguments.get("description");
            String mealType = (String) arguments.get("mealType");
            MealItemResponse result = nutritionService.logMeal(userId, LocalDate.now(), new LogMealRequest(description, mealType));
            return ToolResult.ok("Logged: ~" + result.estimatedCalories() + " kcal, ~" + result.estimatedProteinG() + "g protein");
        } catch (Exception e) {
            return ToolResult.error("Failed to log meal");
        }
    }
}

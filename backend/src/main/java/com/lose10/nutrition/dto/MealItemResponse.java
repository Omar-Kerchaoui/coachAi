package com.lose10.nutrition.dto;

import com.lose10.nutrition.MealItem;

public record MealItemResponse(String rawText, Integer estimatedCalories, Integer estimatedProteinG) {
    public static MealItemResponse from(MealItem item) {
        return new MealItemResponse(item.getRawText(), item.getEstimatedCalories(), item.getEstimatedProteinG());
    }
}

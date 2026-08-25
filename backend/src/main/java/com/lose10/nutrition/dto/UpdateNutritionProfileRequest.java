package com.lose10.nutrition.dto;

import java.util.List;

public record UpdateNutritionProfileRequest(
        List<String> dietaryRestrictions,
        List<String> dislikedFoods,
        Integer targetCalories,
        Integer targetProteinG
) {
}

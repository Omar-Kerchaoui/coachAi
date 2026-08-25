package com.lose10.nutrition.dto;

import jakarta.validation.constraints.NotBlank;

public record LogMealRequest(@NotBlank String description, String mealType) {
}

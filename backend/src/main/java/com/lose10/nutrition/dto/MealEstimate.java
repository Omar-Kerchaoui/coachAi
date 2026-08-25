package com.lose10.nutrition.dto;

public record MealEstimate(int calories, int proteinG) {
    public static final MealEstimate UNKNOWN = new MealEstimate(0, 0);
}

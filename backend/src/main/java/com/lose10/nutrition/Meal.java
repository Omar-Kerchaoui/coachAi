package com.lose10.nutrition;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meals")
public class Meal extends BaseEntity {

    @Column(name = "nutrition_log_id", nullable = false)
    private UUID nutritionLogId;

    @Column(name = "meal_time", nullable = false)
    private Instant mealTime;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType = "SNACK";

    protected Meal() {
    }

    public Meal(UUID nutritionLogId, Instant mealTime, String mealType) {
        this.nutritionLogId = nutritionLogId;
        this.mealTime = mealTime;
        this.mealType = mealType;
    }

    public UUID getNutritionLogId() {
        return nutritionLogId;
    }
}

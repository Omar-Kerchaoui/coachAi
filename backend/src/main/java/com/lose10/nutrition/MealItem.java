package com.lose10.nutrition;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "meal_items")
public class MealItem extends BaseEntity {

    @Column(name = "meal_id", nullable = false)
    private UUID mealId;

    @Column(name = "raw_text", nullable = false, columnDefinition = "text")
    private String rawText;

    @Column(name = "estimated_calories")
    private Integer estimatedCalories;

    @Column(name = "estimated_protein_g")
    private Integer estimatedProteinG;

    @Column(nullable = false, length = 20)
    private String source = "USER_TEXT";

    protected MealItem() {
    }

    public MealItem(UUID mealId, String rawText, Integer estimatedCalories, Integer estimatedProteinG, String source) {
        this.mealId = mealId;
        this.rawText = rawText;
        this.estimatedCalories = estimatedCalories;
        this.estimatedProteinG = estimatedProteinG;
        this.source = source;
    }

    public UUID getMealId() {
        return mealId;
    }

    public String getRawText() {
        return rawText;
    }

    public Integer getEstimatedCalories() {
        return estimatedCalories;
    }

    public Integer getEstimatedProteinG() {
        return estimatedProteinG;
    }
}

package com.lose10.nutrition;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "nutrition_profiles")
public class NutritionProfile extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dietary_restrictions", columnDefinition = "jsonb")
    private List<String> dietaryRestrictions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "disliked_foods", columnDefinition = "jsonb")
    private List<String> dislikedFoods;

    @Column(name = "target_calories")
    private Integer targetCalories;

    @Column(name = "target_protein_g")
    private Integer targetProteinG;

    protected NutritionProfile() {
    }

    public NutritionProfile(UUID userId) {
        this.userId = userId;
    }

    public void update(List<String> dietaryRestrictions, List<String> dislikedFoods, Integer targetCalories, Integer targetProteinG) {
        this.dietaryRestrictions = dietaryRestrictions;
        this.dislikedFoods = dislikedFoods;
        this.targetCalories = targetCalories;
        this.targetProteinG = targetProteinG;
    }

    public UUID getUserId() {
        return userId;
    }

    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public List<String> getDislikedFoods() {
        return dislikedFoods;
    }

    public Integer getTargetCalories() {
        return targetCalories;
    }

    public Integer getTargetProteinG() {
        return targetProteinG;
    }
}

package com.lose10.goal;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "goals")
public class Goal extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 30)
    private GoalType goalType = GoalType.LOSE_WEIGHT;

    @Column(name = "starting_weight_kg", nullable = false)
    private BigDecimal startingWeightKg;

    @Column(name = "target_weight_kg", nullable = false)
    private BigDecimal targetWeightKg;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status = GoalStatus.ACTIVE;

    protected Goal() {
    }

    public Goal(UUID userId, BigDecimal startingWeightKg, BigDecimal targetWeightKg, LocalDate targetDate) {
        this.userId = userId;
        this.startingWeightKg = startingWeightKg;
        this.targetWeightKg = targetWeightKg;
        this.targetDate = targetDate;
        this.status = GoalStatus.ACTIVE;
    }

    public void updateTarget(BigDecimal newTargetWeightKg, LocalDate newTargetDate) {
        if (newTargetWeightKg != null) {
            this.targetWeightKg = newTargetWeightKg;
        }
        if (newTargetDate != null) {
            this.targetDate = newTargetDate;
        }
    }

    public void markAchieved() {
        this.status = GoalStatus.ACHIEVED;
    }

    public void pause() {
        this.status = GoalStatus.PAUSED;
    }

    public void resume() {
        this.status = GoalStatus.ACTIVE;
    }

    public BigDecimal weightToLoseKg() {
        return startingWeightKg.subtract(targetWeightKg);
    }

    public UUID getUserId() {
        return userId;
    }

    public GoalType getGoalType() {
        return goalType;
    }

    public BigDecimal getStartingWeightKg() {
        return startingWeightKg;
    }

    public BigDecimal getTargetWeightKg() {
        return targetWeightKg;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public GoalStatus getStatus() {
        return status;
    }
}

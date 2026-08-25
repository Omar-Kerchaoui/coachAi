package com.lose10.goal.dto;

import com.lose10.goal.Goal;
import com.lose10.goal.GoalStatus;
import com.lose10.goal.GoalType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        GoalType goalType,
        BigDecimal startingWeightKg,
        BigDecimal targetWeightKg,
        BigDecimal weightToLoseKg,
        LocalDate targetDate,
        GoalStatus status
) {
    public static GoalResponse from(Goal goal) {
        return new GoalResponse(
                goal.getId(), goal.getGoalType(), goal.getStartingWeightKg(), goal.getTargetWeightKg(),
                goal.weightToLoseKg(), goal.getTargetDate(), goal.getStatus()
        );
    }
}

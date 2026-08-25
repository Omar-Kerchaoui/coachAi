package com.lose10.goal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGoalRequest(
        @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal targetWeightKg,
        @Future LocalDate targetDate
) {
}

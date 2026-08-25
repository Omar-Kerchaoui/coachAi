package com.lose10.weight.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.Instant;

public record LogWeightRequest(
        @NotNull @DecimalMin("30.0") @DecimalMax("300.0") BigDecimal weightKg,
        @PastOrPresent Instant recordedAt,
        boolean confirmImplausible
) {
}

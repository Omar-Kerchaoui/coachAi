package com.lose10.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LogWorkoutSessionRequest(
        @NotNull UUID workoutId,
        @Min(1) @Max(10) Integer perceivedEffort,
        String notes
) {
}

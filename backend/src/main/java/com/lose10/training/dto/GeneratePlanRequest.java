package com.lose10.training.dto;

import com.lose10.training.TrainingEnvironment;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GeneratePlanRequest(@NotNull UUID goalId, @NotNull TrainingEnvironment environment) {
}

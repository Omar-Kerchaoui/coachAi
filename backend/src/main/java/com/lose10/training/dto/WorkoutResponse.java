package com.lose10.training.dto;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public record WorkoutResponse(UUID id, DayOfWeek dayOfWeek, String workoutType, List<WorkoutExerciseResponse> exercises) {
}

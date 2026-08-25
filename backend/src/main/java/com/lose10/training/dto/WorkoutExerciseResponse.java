package com.lose10.training.dto;

public record WorkoutExerciseResponse(String exerciseName, Integer sets, Integer reps, Integer durationSec, String intensity) {
}

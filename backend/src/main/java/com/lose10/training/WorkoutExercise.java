package com.lose10.training;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "workout_exercises")
public class WorkoutExercise extends BaseEntity {

    @Column(name = "workout_id", nullable = false)
    private UUID workoutId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column
    private Integer sets;

    @Column
    private Integer reps;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "rest_sec")
    private Integer restSec;

    @Column(length = 20)
    private String intensity;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    protected WorkoutExercise() {
    }

    public WorkoutExercise(UUID workoutId, UUID exerciseId, Integer sets, Integer reps, Integer durationSec, Integer restSec, String intensity, int orderIndex) {
        this.workoutId = workoutId;
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.reps = reps;
        this.durationSec = durationSec;
        this.restSec = restSec;
        this.intensity = intensity;
        this.orderIndex = orderIndex;
    }

    public UUID getWorkoutId() {
        return workoutId;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public Integer getSets() {
        return sets;
    }

    public Integer getReps() {
        return reps;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}

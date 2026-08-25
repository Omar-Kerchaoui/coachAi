package com.lose10.training;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.util.UUID;

@Entity
@Table(name = "workouts")
public class Workout extends BaseEntity {

    @Column(name = "plan_version_id", nullable = false)
    private UUID planVersionId;

    @Column(name = "day_of_week", nullable = false, length = 10)
    private String dayOfWeek;

    @Column(name = "workout_type", nullable = false, length = 30)
    private String workoutType = "FULL_BODY";

    protected Workout() {
    }

    public Workout(UUID planVersionId, DayOfWeek dayOfWeek, String workoutType) {
        this.planVersionId = planVersionId;
        this.dayOfWeek = dayOfWeek.name();
        this.workoutType = workoutType;
    }

    public UUID getPlanVersionId() {
        return planVersionId;
    }

    public DayOfWeek getDayOfWeek() {
        return DayOfWeek.valueOf(dayOfWeek);
    }

    public String getWorkoutType() {
        return workoutType;
    }
}

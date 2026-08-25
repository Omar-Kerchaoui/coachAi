package com.lose10.training;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions")
public class WorkoutSession extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workout_id", nullable = false)
    private UUID workoutId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "perceived_effort")
    private Integer perceivedEffort;

    @Column(columnDefinition = "text")
    private String notes;

    protected WorkoutSession() {
    }

    public WorkoutSession(UUID userId, UUID workoutId, Instant completedAt, Integer perceivedEffort, String notes) {
        this.userId = userId;
        this.workoutId = workoutId;
        this.completedAt = completedAt;
        this.perceivedEffort = perceivedEffort;
        this.notes = notes;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getWorkoutId() {
        return workoutId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Integer getPerceivedEffort() {
        return perceivedEffort;
    }
}

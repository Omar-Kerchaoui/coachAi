package com.lose10.goal;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "goal_milestones")
public class GoalMilestone extends BaseEntity {

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(nullable = false)
    private int percentage;

    @Column(name = "reached_at")
    private Instant reachedAt;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    protected GoalMilestone() {
    }

    public GoalMilestone(UUID goalId, int percentage) {
        this.goalId = goalId;
        this.percentage = percentage;
    }

    public void markReached() {
        this.reachedAt = Instant.now();
    }

    public void markNotified() {
        this.notifiedAt = Instant.now();
    }

    public boolean isReached() {
        return reachedAt != null;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public int getPercentage() {
        return percentage;
    }

    public Instant getReachedAt() {
        return reachedAt;
    }
}

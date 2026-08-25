package com.lose10.training;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "training_plans")
public class TrainingPlan extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "goal_id")
    private UUID goalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingPlanStatus status = TrainingPlanStatus.ACTIVE;

    @Column(name = "current_version_id")
    private UUID currentVersionId;

    protected TrainingPlan() {
    }

    public TrainingPlan(UUID userId, UUID goalId) {
        this.userId = userId;
        this.goalId = goalId;
    }

    public void setCurrentVersionId(UUID versionId) {
        this.currentVersionId = versionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public TrainingPlanStatus getStatus() {
        return status;
    }

    public UUID getCurrentVersionId() {
        return currentVersionId;
    }
}

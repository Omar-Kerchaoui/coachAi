package com.lose10.training;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "training_plan_versions")
public class TrainingPlanVersion extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingEnvironment environment = TrainingEnvironment.HOME;

    @Column(name = "reason_for_change", columnDefinition = "text")
    private String reasonForChange;

    protected TrainingPlanVersion() {
    }

    public TrainingPlanVersion(UUID planId, int versionNo, TrainingEnvironment environment, String reasonForChange) {
        this.planId = planId;
        this.versionNo = versionNo;
        this.environment = environment;
        this.reasonForChange = reasonForChange;
    }

    public UUID getPlanId() {
        return planId;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public TrainingEnvironment getEnvironment() {
        return environment;
    }
}

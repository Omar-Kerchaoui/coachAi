package com.lose10.admin;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_interventions")
public class HumanIntervention extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "ended_at")
    private Instant endedAt;

    protected HumanIntervention() {
    }

    public HumanIntervention(UUID userId, UUID adminId, String reason) {
        this.userId = userId;
        this.adminId = adminId;
        this.reason = reason;
    }

    public void end() {
        this.endedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getEndedAt() {
        return endedAt;
    }
}

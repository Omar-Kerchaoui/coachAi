package com.lose10.coaching;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * {@code style} mirrors {@code user_preferences.coach_style} (Epic 3) at
 * creation time but is NOT the authoritative source — style changes go
 * through {@code PATCH /users/me/preferences}, not this module. This
 * module owns exactly one new thing: {@link CoachingMode}, checked at the
 * orchestrator's entry gate before any AI auto-response (docs/SECURITY.md
 * §11) — never after generation starts, to avoid a race between AI and
 * human both replying to the same message.
 */
@Entity
@Table(name = "coaching_states")
public class CoachingState extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoachingMode mode = CoachingMode.AI_ACTIVE;

    @Column(nullable = false, length = 20)
    private String style = "BALANCED";

    @Column(name = "paused_reason", columnDefinition = "text")
    private String pausedReason;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected CoachingState() {
    }

    public CoachingState(UUID userId) {
        this.userId = userId;
    }

    public void setMode(CoachingMode mode, String pausedReason, UUID updatedBy) {
        this.mode = mode;
        this.pausedReason = mode == CoachingMode.PAUSED ? pausedReason : null;
        this.updatedBy = updatedBy;
    }

    public boolean allowsAutomaticAiResponse() {
        return mode == CoachingMode.AI_ACTIVE;
    }

    public UUID getUserId() {
        return userId;
    }

    public CoachingMode getMode() {
        return mode;
    }

    public String getPausedReason() {
        return pausedReason;
    }
}

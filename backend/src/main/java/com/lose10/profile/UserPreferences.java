package com.lose10.profile;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UnitSystem units = UnitSystem.METRIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_style", nullable = false, length = 20)
    private CoachStyle coachStyle = CoachStyle.BALANCED;

    protected UserPreferences() {
    }

    public UserPreferences(UUID userId) {
        this.userId = userId;
    }

    public void update(UnitSystem units, CoachStyle coachStyle) {
        if (units != null) {
            this.units = units;
        }
        if (coachStyle != null) {
            this.coachStyle = coachStyle;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public UnitSystem getUnits() {
        return units;
    }

    public CoachStyle getCoachStyle() {
        return coachStyle;
    }
}

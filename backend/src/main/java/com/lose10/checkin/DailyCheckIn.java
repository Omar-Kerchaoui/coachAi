package com.lose10.checkin;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A structured daily self-report — distinct from {@link CheckIn}, which
 * tracks the scheduled interaction event itself (was it sent, was it
 * completed, free-form JSON responses). This table exists so mood/energy/
 * adherence are queryable columns (e.g. for behavior pattern detection or
 * admin analytics), not just opaque JSON.
 */
@Entity
@Table(name = "daily_check_ins")
public class DailyCheckIn extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(length = 20)
    private String mood;

    @Column(length = 20)
    private String energy;

    @Column(name = "adherence_self_report", length = 20)
    private String adherenceSelfReport;

    protected DailyCheckIn() {
    }

    public DailyCheckIn(UUID userId, LocalDate checkInDate, String mood, String energy, String adherenceSelfReport) {
        this.userId = userId;
        this.checkInDate = checkInDate;
        this.mood = mood;
        this.energy = energy;
        this.adherenceSelfReport = adherenceSelfReport;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public String getMood() {
        return mood;
    }

    public String getEnergy() {
        return energy;
    }

    public String getAdherenceSelfReport() {
        return adherenceSelfReport;
    }
}

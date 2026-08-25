package com.lose10.notification;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "morning_checkin_time")
    private LocalTime morningCheckinTime = LocalTime.of(8, 0);

    @Column(name = "evening_checkin_time")
    private LocalTime eveningCheckinTime = LocalTime.of(20, 0);

    @Column(name = "workout_reminder_enabled", nullable = false)
    private boolean workoutReminderEnabled = true;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart = LocalTime.of(21, 0);

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd = LocalTime.of(7, 0);

    protected NotificationPreferences() {
    }

    public NotificationPreferences(UUID userId) {
        this.userId = userId;
    }

    public void update(LocalTime morningCheckinTime, LocalTime eveningCheckinTime, Boolean workoutReminderEnabled, LocalTime quietHoursStart, LocalTime quietHoursEnd) {
        if (morningCheckinTime != null) this.morningCheckinTime = morningCheckinTime;
        if (eveningCheckinTime != null) this.eveningCheckinTime = eveningCheckinTime;
        if (workoutReminderEnabled != null) this.workoutReminderEnabled = workoutReminderEnabled;
        if (quietHoursStart != null) this.quietHoursStart = quietHoursStart;
        if (quietHoursEnd != null) this.quietHoursEnd = quietHoursEnd;
    }

    /** Handles a window that wraps midnight (e.g. 21:00-07:00). */
    public boolean isWithinQuietHours(LocalTime localTime) {
        if (quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            return !localTime.isBefore(quietHoursStart) && localTime.isBefore(quietHoursEnd);
        }
        return !localTime.isBefore(quietHoursStart) || localTime.isBefore(quietHoursEnd);
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalTime getMorningCheckinTime() {
        return morningCheckinTime;
    }

    public LocalTime getEveningCheckinTime() {
        return eveningCheckinTime;
    }

    public boolean isWorkoutReminderEnabled() {
        return workoutReminderEnabled;
    }
}

package com.lose10.profile;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
public class UserSettings extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notification_frequency", nullable = false, length = 20)
    private String notificationFrequency = "NORMAL";

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    protected UserSettings() {
    }

    public UserSettings(UUID userId) {
        this.userId = userId;
    }

    public void update(String notificationFrequency, LocalTime quietHoursStart, LocalTime quietHoursEnd) {
        if (notificationFrequency != null) {
            this.notificationFrequency = notificationFrequency;
        }
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getNotificationFrequency() {
        return notificationFrequency;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }
}

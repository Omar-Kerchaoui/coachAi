package com.lose10.notification;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationPreferencesTest {

    @Test
    void quietHoursWrappingMidnightCoversBothSidesOfMidnight() {
        NotificationPreferences prefs = new NotificationPreferences(UUID.randomUUID());
        prefs.update(null, null, null, LocalTime.of(21, 0), LocalTime.of(7, 0));

        assertThat(prefs.isWithinQuietHours(LocalTime.of(22, 30))).isTrue(); // late evening
        assertThat(prefs.isWithinQuietHours(LocalTime.of(2, 0))).isTrue();   // past midnight
        assertThat(prefs.isWithinQuietHours(LocalTime.of(6, 59))).isTrue(); // just before end
        assertThat(prefs.isWithinQuietHours(LocalTime.of(7, 0))).isFalse(); // exactly at end, exclusive
        assertThat(prefs.isWithinQuietHours(LocalTime.of(12, 0))).isFalse(); // midday
        assertThat(prefs.isWithinQuietHours(LocalTime.of(20, 59))).isFalse(); // just before start
    }

    @Test
    void quietHoursNotWrappingMidnightIsAStraightforwardRange() {
        NotificationPreferences prefs = new NotificationPreferences(UUID.randomUUID());
        prefs.update(null, null, null, LocalTime.of(1, 0), LocalTime.of(5, 0));

        assertThat(prefs.isWithinQuietHours(LocalTime.of(3, 0))).isTrue();
        assertThat(prefs.isWithinQuietHours(LocalTime.of(12, 0))).isFalse();
    }
}

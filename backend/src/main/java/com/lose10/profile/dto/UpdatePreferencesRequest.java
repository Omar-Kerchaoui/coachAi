package com.lose10.profile.dto;

import com.lose10.profile.CoachStyle;
import com.lose10.profile.UnitSystem;

import java.time.LocalTime;

public record UpdatePreferencesRequest(
        UnitSystem units,
        CoachStyle coachStyle,
        String notificationFrequency,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}

package com.lose10.profile.dto;

import com.lose10.profile.CoachStyle;
import com.lose10.profile.UnitSystem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ProfileResponse(
        UUID userId,
        String email,
        String displayName,
        LocalDate dateOfBirth,
        String sex,
        BigDecimal heightCm,
        String country,
        String timezone,
        String locale,
        UnitSystem units,
        CoachStyle coachStyle,
        String notificationFrequency,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}

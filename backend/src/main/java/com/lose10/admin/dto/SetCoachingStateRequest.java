package com.lose10.admin.dto;

import com.lose10.coaching.CoachingMode;
import jakarta.validation.constraints.NotNull;

public record SetCoachingStateRequest(@NotNull CoachingMode mode, String reason) {
}

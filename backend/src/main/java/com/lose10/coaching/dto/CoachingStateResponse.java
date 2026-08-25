package com.lose10.coaching.dto;

import com.lose10.coaching.CoachingMode;
import com.lose10.coaching.CoachingState;

public record CoachingStateResponse(CoachingMode mode, String pausedReason) {
    public static CoachingStateResponse from(CoachingState state) {
        return new CoachingStateResponse(state.getMode(), state.getPausedReason());
    }
}

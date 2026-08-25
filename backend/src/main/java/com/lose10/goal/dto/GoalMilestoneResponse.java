package com.lose10.goal.dto;

import com.lose10.goal.GoalMilestone;

import java.time.Instant;
import java.util.UUID;

public record GoalMilestoneResponse(UUID id, int percentage, Instant reachedAt) {
    public static GoalMilestoneResponse from(GoalMilestone milestone) {
        return new GoalMilestoneResponse(milestone.getId(), milestone.getPercentage(), milestone.getReachedAt());
    }
}

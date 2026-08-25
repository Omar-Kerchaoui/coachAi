package com.lose10.goal;

import com.lose10.goal.dto.CreateGoalRequest;
import com.lose10.goal.dto.GoalResponse;
import com.lose10.goal.dto.UpdateGoalRequest;
import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoalService {

    private static final List<Integer> MILESTONE_PERCENTAGES = List.of(5, 25, 50, 75, 90, 100);

    private final GoalRepository goalRepository;
    private final GoalMilestoneRepository milestoneRepository;
    private final GoalEventRepository eventRepository;

    public GoalService(GoalRepository goalRepository, GoalMilestoneRepository milestoneRepository, GoalEventRepository eventRepository) {
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public GoalResponse createGoal(UUID userId, CreateGoalRequest request) {
        if (goalRepository.existsByUserIdAndStatus(userId, GoalStatus.ACTIVE)) {
            throw ApiException.businessRule("An active goal already exists — pause or complete it before creating a new one");
        }
        if (request.targetWeightKg().compareTo(request.startingWeightKg()) >= 0) {
            throw ApiException.businessRule("Target weight must be lower than starting weight for a weight-loss goal");
        }
        Goal goal = new Goal(userId, request.startingWeightKg(), request.targetWeightKg(), request.targetDate());
        goal = goalRepository.save(goal);

        for (int pct : MILESTONE_PERCENTAGES) {
            milestoneRepository.save(new GoalMilestone(goal.getId(), pct));
        }
        eventRepository.save(new GoalEvent(goal.getId(), "GOAL_CREATED", Map.of(
                "startingWeightKg", request.startingWeightKg(),
                "targetWeightKg", request.targetWeightKg()
        )));

        return GoalResponse.from(goal);
    }

    @Transactional
    public GoalResponse updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request) {
        Goal goal = requireOwnedGoal(goalId, userId);

        if (request.targetWeightKg() != null && request.targetWeightKg().compareTo(goal.getStartingWeightKg()) >= 0) {
            throw ApiException.businessRule("Target weight must be lower than starting weight for a weight-loss goal");
        }

        eventRepository.save(new GoalEvent(goal.getId(), "GOAL_TARGET_CHANGED", Map.of(
                "previousTargetWeightKg", goal.getTargetWeightKg(),
                "previousTargetDate", String.valueOf(goal.getTargetDate())
        )));

        goal.updateTarget(request.targetWeightKg(), request.targetDate());
        return GoalResponse.from(goal);
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> listGoals(UUID userId) {
        return goalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(GoalResponse::from).toList();
    }

    /**
     * Used by the nightly progress snapshot job (Epic 3, L10-309) to know
     * which users currently need a snapshot computed.
     */
    @Transactional(readOnly = true)
    public List<Goal> listActiveGoals() {
        return goalRepository.findByStatus(GoalStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Goal requireActiveGoal(UUID userId) {
        return goalRepository.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)
                .orElseThrow(() -> ApiException.notFound("No active goal"));
    }

    @Transactional(readOnly = true)
    public List<GoalMilestone> listMilestones(UUID userId, UUID goalId) {
        requireOwnedGoal(goalId, userId);
        return milestoneRepository.findByGoalIdOrderByPercentageAsc(goalId);
    }

    private Goal requireOwnedGoal(UUID goalId, UUID userId) {
        return goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> ApiException.notFound("Goal not found"));
    }
}

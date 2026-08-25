package com.lose10.goal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalMilestoneRepository extends JpaRepository<GoalMilestone, UUID> {
    List<GoalMilestone> findByGoalIdOrderByPercentageAsc(UUID goalId);
}

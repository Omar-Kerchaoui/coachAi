package com.lose10.goal;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges goals on account deletion (docs/backlog L10-1004). Milestones and
 * events cascade at the DB level (goal_milestones/goal_events →
 * goals ON DELETE CASCADE), so deleting the parent Goal rows is sufficient.
 */
@Component
public class AccountDeletionGoalPurger {

    private final GoalRepository goalRepository;

    public AccountDeletionGoalPurger(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        goalRepository.deleteByUserId(event.userId());
    }
}

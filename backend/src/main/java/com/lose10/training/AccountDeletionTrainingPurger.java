package com.lose10.training;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges training plans and logged sessions on account deletion
 * (docs/backlog L10-1004). Plan versions/workouts/workout-exercises
 * cascade at the DB level from training_plans, so deleting the plan rows
 * is sufficient for those.
 */
@Component
public class AccountDeletionTrainingPurger {

    private final TrainingPlanRepository planRepository;
    private final WorkoutSessionRepository sessionRepository;

    public AccountDeletionTrainingPurger(TrainingPlanRepository planRepository, WorkoutSessionRepository sessionRepository) {
        this.planRepository = planRepository;
        this.sessionRepository = sessionRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        planRepository.deleteByUserId(event.userId());
        sessionRepository.deleteByUserId(event.userId());
    }
}

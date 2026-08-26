package com.lose10.nutrition;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges nutrition profile and logs on account deletion (docs/backlog
 * L10-1004). Meals/meal-items cascade at the DB level from
 * nutrition_logs, so deleting the log rows is sufficient for those.
 */
@Component
public class AccountDeletionNutritionPurger {

    private final NutritionProfileRepository profileRepository;
    private final NutritionLogRepository logRepository;

    public AccountDeletionNutritionPurger(NutritionProfileRepository profileRepository, NutritionLogRepository logRepository) {
        this.profileRepository = profileRepository;
        this.logRepository = logRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        profileRepository.deleteByUserId(event.userId());
        logRepository.deleteByUserId(event.userId());
    }
}

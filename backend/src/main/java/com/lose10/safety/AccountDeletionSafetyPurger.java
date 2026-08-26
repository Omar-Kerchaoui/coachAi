package com.lose10.safety;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges safety events on account deletion (docs/backlog L10-1004).
 * Unlike audit_logs, safety events can hold raw {@code source_text} —
 * a direct excerpt of what the user wrote — so they're purged rather
 * than retained, even though they're moderation-adjacent records.
 */
@Component
public class AccountDeletionSafetyPurger {

    private final SafetyEventRepository repository;

    public AccountDeletionSafetyPurger(SafetyEventRepository repository) {
        this.repository = repository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        repository.deleteByUserId(event.userId());
    }
}

package com.lose10.behavior;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges behavior patterns and events on account deletion (docs/backlog
 * L10-1004).
 */
@Component
public class AccountDeletionBehaviorPurger {

    private final BehaviorPatternRepository patternRepository;
    private final BehaviorEventRepository eventRepository;

    public AccountDeletionBehaviorPurger(BehaviorPatternRepository patternRepository, BehaviorEventRepository eventRepository) {
        this.patternRepository = patternRepository;
        this.eventRepository = eventRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        patternRepository.deleteByUserId(event.userId());
        eventRepository.deleteByUserId(event.userId());
    }
}

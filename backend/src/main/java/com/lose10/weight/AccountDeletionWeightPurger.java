package com.lose10.weight;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges weight/body-measurement history on account deletion
 * (docs/backlog L10-1004).
 */
@Component
public class AccountDeletionWeightPurger {

    private final WeightEntryRepository weightEntryRepository;
    private final BodyMeasurementRepository bodyMeasurementRepository;

    public AccountDeletionWeightPurger(WeightEntryRepository weightEntryRepository, BodyMeasurementRepository bodyMeasurementRepository) {
        this.weightEntryRepository = weightEntryRepository;
        this.bodyMeasurementRepository = bodyMeasurementRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        weightEntryRepository.deleteByUserId(event.userId());
        bodyMeasurementRepository.deleteByUserId(event.userId());
    }
}

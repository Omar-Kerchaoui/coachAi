package com.lose10.profile;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges profile/preferences/settings/consent on account deletion
 * (docs/backlog L10-1004) — the mirror image of
 * {@link RegistrationProfileInitializer}.
 */
@Component
public class AccountDeletionProfilePurger {

    private final UserProfileRepository profileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserSettingsRepository settingsRepository;
    private final UserConsentRepository consentRepository;

    public AccountDeletionProfilePurger(
            UserProfileRepository profileRepository,
            UserPreferencesRepository preferencesRepository,
            UserSettingsRepository settingsRepository,
            UserConsentRepository consentRepository
    ) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.settingsRepository = settingsRepository;
        this.consentRepository = consentRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        profileRepository.deleteByUserId(event.userId());
        preferencesRepository.deleteByUserId(event.userId());
        settingsRepository.deleteByUserId(event.userId());
        consentRepository.deleteByUserId(event.userId());
    }
}

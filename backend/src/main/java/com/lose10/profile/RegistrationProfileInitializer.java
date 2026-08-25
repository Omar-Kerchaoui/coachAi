package com.lose10.profile;

import com.lose10.auth.event.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default profile/preferences/settings rows a user needs to
 * exist as soon as they register, before onboarding fills in real values.
 */
@Component
public class RegistrationProfileInitializer {

    private final UserProfileRepository profileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserSettingsRepository settingsRepository;

    public RegistrationProfileInitializer(
            UserProfileRepository profileRepository,
            UserPreferencesRepository preferencesRepository,
            UserSettingsRepository settingsRepository
    ) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.settingsRepository = settingsRepository;
    }

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        profileRepository.save(new UserProfile(event.userId(), "UTC", "en"));
        preferencesRepository.save(new UserPreferences(event.userId()));
        settingsRepository.save(new UserSettings(event.userId()));
    }
}

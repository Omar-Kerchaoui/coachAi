package com.lose10.profile;

import com.lose10.auth.UserDirectory;
import com.lose10.auth.dto.UserSummary;
import com.lose10.platform.error.ApiException;
import com.lose10.profile.dto.ConsentRequest;
import com.lose10.profile.dto.ProfileResponse;
import com.lose10.profile.dto.UpdatePreferencesRequest;
import com.lose10.profile.dto.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final UserSettingsRepository settingsRepository;
    private final UserConsentRepository consentRepository;
    private final UserDirectory userDirectory;

    public ProfileService(
            UserProfileRepository profileRepository,
            UserPreferencesRepository preferencesRepository,
            UserSettingsRepository settingsRepository,
            UserConsentRepository consentRepository,
            UserDirectory userDirectory
    ) {
        this.profileRepository = profileRepository;
        this.preferencesRepository = preferencesRepository;
        this.settingsRepository = settingsRepository;
        this.consentRepository = consentRepository;
        this.userDirectory = userDirectory;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        UserSummary user = userDirectory.getSummary(userId);
        UserProfile profile = requireProfile(userId);
        UserPreferences preferences = requirePreferences(userId);
        UserSettings settings = requireSettings(userId);

        return new ProfileResponse(
                user.id(), user.email(),
                profile.getDisplayName(), profile.getDateOfBirth(), profile.getSex(), profile.getHeightCm(),
                profile.getCountry(), profile.getTimezone(), profile.getLocale(),
                preferences.getUnits(), preferences.getCoachStyle(),
                settings.getNotificationFrequency(), settings.getQuietHoursStart(), settings.getQuietHoursEnd()
        );
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = requireProfile(userId);
        profile.update(request.displayName(), request.dateOfBirth(), request.sex(), request.heightCm(), request.country(), request.timezone());
        return getProfile(userId);
    }

    @Transactional
    public ProfileResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        UserPreferences preferences = requirePreferences(userId);
        preferences.update(request.units(), request.coachStyle());

        UserSettings settings = requireSettings(userId);
        settings.update(request.notificationFrequency(), request.quietHoursStart(), request.quietHoursEnd());

        return getProfile(userId);
    }

    @Transactional
    public void recordConsent(UUID userId, ConsentRequest request) {
        consentRepository.save(new UserConsent(userId, request.consentType(), request.version(), request.source()));
    }

    @Transactional(readOnly = true)
    public List<UserConsent> listConsents(UUID userId) {
        return consentRepository.findByUserId(userId);
    }

    @Transactional
    public void revokeConsent(UUID userId, String consentType) {
        List<UserConsent> consents = consentRepository.findByUserId(userId);
        for (UserConsent consent : consents) {
            if (consent.getConsentType().equals(consentType) && consent.isActive()) {
                consent.revoke();
            }
        }
    }

    UserProfile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Profile not found"));
    }

    private UserPreferences requirePreferences(UUID userId) {
        return preferencesRepository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Preferences not found"));
    }

    private UserSettings requireSettings(UUID userId) {
        return settingsRepository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Settings not found"));
    }
}

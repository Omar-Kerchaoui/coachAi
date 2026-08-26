package com.lose10.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    List<UserConsent> findByUserId(UUID userId);

    Optional<UserConsent> findFirstByUserIdAndConsentTypeAndRevokedAtIsNullOrderByGrantedAtDesc(UUID userId, String consentType);

    void deleteByUserId(UUID userId);
}

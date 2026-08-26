package com.lose10.profile.dto;

import com.lose10.profile.UserConsent;

import java.time.Instant;

public record ConsentResponse(String consentType, String version, Instant grantedAt, Instant revokedAt, boolean active) {
    public static ConsentResponse from(UserConsent consent) {
        return new ConsentResponse(consent.getConsentType(), consent.getVersion(), consent.getGrantedAt(), consent.getRevokedAt(), consent.isActive());
    }
}

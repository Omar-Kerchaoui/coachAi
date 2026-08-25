package com.lose10.profile;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only: a revoke sets {@link #revokedAt} rather than deleting the
 * row, preserving the full consent history (docs/SECURITY.md §8).
 */
@Entity
@Table(name = "user_consents")
public class UserConsent extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_type", nullable = false, length = 50)
    private String consentType;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(length = 50)
    private String source;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected UserConsent() {
    }

    public UserConsent(UUID userId, String consentType, String version, String source) {
        this.userId = userId;
        this.consentType = consentType;
        this.version = version;
        this.source = source;
        this.grantedAt = Instant.now();
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public boolean isActive() {
        return revokedAt == null;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getConsentType() {
        return consentType;
    }

    public String getVersion() {
        return version;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}

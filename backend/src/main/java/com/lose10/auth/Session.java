package com.lose10.auth;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected Session() {
    }

    public Session(UUID userId, String ip, String userAgent, Instant expiresAt) {
        this.userId = userId;
        this.ip = ip;
        this.userAgent = userAgent;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}

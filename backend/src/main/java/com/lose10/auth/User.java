package com.lose10.auth;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User extends MutableEntity {

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_e164")
    private String phoneE164;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    protected User() {
    }

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void markEmailVerified(Instant at) {
        this.emailVerifiedAt = at;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void softDelete() {
        this.status = UserStatus.DELETED;
    }

    /**
     * Scrubs PII from the row itself while leaving it in place (docs/backlog
     * L10-1004) — subscriptions/payments/audit_logs reference this row and
     * must survive deletion per their own retention requirements, so the
     * account can never be hard-deleted, only anonymized. The UUID suffix
     * guarantees no collision with the unique email index.
     */
    public void anonymize() {
        this.email = "deleted-" + getId() + "@deleted.invalid";
        this.phoneE164 = null;
        this.status = UserStatus.DELETED;
    }

    public void changePasswordHash(String newHash) {
        this.passwordHash = newHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public void setPhoneE164(String phoneE164) {
        this.phoneE164 = phoneE164;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }
}

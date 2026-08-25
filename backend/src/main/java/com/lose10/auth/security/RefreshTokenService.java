package com.lose10.auth.security;

import com.lose10.auth.RefreshToken;
import com.lose10.auth.RefreshTokenRepository;
import com.lose10.platform.error.ApiException;
import com.lose10.platform.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Rotating refresh tokens with reuse detection (docs/SECURITY.md §1). A
 * refresh token is single-use: {@link #rotate} always issues a new token
 * and revokes the one presented. If a token that was already revoked is
 * presented again, that's a signal of possible theft (someone replayed a
 * stolen token after the legitimate client already rotated past it) — the
 * entire family is revoked, forcing re-authentication.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final TokenHasher hasher;
    private final Duration ttl;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            TokenHasher hasher,
            @Value("${lose10.auth.refresh-token-ttl-days}") long ttlDays
    ) {
        this.repository = repository;
        this.hasher = hasher;
        this.ttl = Duration.ofDays(ttlDays);
    }

    @Transactional
    public IssuedToken issueNewFamily(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        String hash = hasher.hash(rawToken);
        RefreshToken existing = repository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (existing.isRevoked()) {
            revokeFamily(existing.getFamilyId());
            throw new ApiException(ErrorCode.TOKEN_REUSE_DETECTED, HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse detected — all sessions in this family have been revoked");
        }
        if (existing.isExpired()) {
            throw new ApiException(ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        IssuedToken next = issue(existing.getUserId(), existing.getFamilyId());
        existing.revoke(next.tokenId());
        return new RotationResult(existing.getUserId(), next);
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = hasher.hash(rawToken);
        repository.findByTokenHash(hash).ifPresent(t -> {
            if (!t.isRevoked()) {
                t.revoke(null);
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> tokens = repository.findByUserId(userId);
        for (RefreshToken t : tokens) {
            if (!t.isRevoked()) {
                t.revoke(null);
            }
        }
    }

    private void revokeFamily(UUID familyId) {
        List<RefreshToken> tokens = repository.findByFamilyId(familyId);
        for (RefreshToken t : tokens) {
            if (!t.isRevoked()) {
                t.revoke(null);
            }
        }
    }

    private IssuedToken issue(UUID userId, UUID familyId) {
        String raw = hasher.generateRawToken();
        String hash = hasher.hash(raw);
        Instant expiresAt = Instant.now().plus(ttl);
        RefreshToken saved = repository.save(new RefreshToken(userId, hash, familyId, expiresAt));
        return new IssuedToken(raw, saved.getId(), familyId);
    }

    public record IssuedToken(String rawToken, UUID tokenId, UUID familyId) {
    }

    public record RotationResult(UUID userId, IssuedToken issuedToken) {
    }
}

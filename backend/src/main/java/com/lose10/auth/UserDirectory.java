package com.lose10.auth;

import com.lose10.auth.dto.UserSummary;

import java.util.UUID;

/**
 * The narrow read interface other modules (profile, in particular) use to
 * resolve identity-owned fields without reaching into auth's repositories
 * directly (docs/ARCHITECTURE.md §4 module boundary rule).
 */
public interface UserDirectory {
    UserSummary getSummary(UUID userId);

    boolean isEmailVerified(UUID userId);
}

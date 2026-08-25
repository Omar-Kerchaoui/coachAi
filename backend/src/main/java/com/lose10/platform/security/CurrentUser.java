package com.lose10.platform.security;

import com.lose10.platform.error.ApiException;
import com.lose10.platform.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * The single, authoritative way to obtain the authenticated user's ID
 * anywhere in the codebase — repository lookups and AI tool implementations
 * must use this rather than trusting a client- or model-supplied user ID
 * (see docs/SECURITY.md §3 and docs/AI_ARCHITECTURE.md §4).
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED, "No authenticated user in context");
        }
        return principal.userId();
    }

    public static UUID idOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }
}

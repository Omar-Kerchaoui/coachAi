package com.lose10.platform.security;

import java.util.UUID;

/**
 * The Spring Security principal type used across the app: just enough to
 * resolve the authenticated user's ID without a database round-trip on
 * every request. Roles are carried separately as granted authorities.
 */
public record AuthenticatedPrincipal(UUID userId, String email) {
}

package com.lose10.auth.event;

import java.util.UUID;

/**
 * Fan-out signal for the account-deletion cascade (docs/backlog L10-1004):
 * every domain module that can hold this user's data listens for this and
 * purges its own tables, mirroring the registration fan-out
 * ({@code RegistrationProfileInitializer} etc.) in reverse. Kept as a
 * generic event rather than {@code AuthService} depending on every domain
 * module directly — that would invert the module boundary the rest of the
 * app follows (docs/ARCHITECTURE.md §4).
 */
public record UserDeletionRequestedEvent(UUID userId) {
}

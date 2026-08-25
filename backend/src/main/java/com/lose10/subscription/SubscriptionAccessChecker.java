package com.lose10.subscription;

import java.util.UUID;

/**
 * The narrow read interface other modules (WhatsApp, in particular) use to
 * gate coaching access without reaching into subscription's repositories
 * directly (docs/ARCHITECTURE.md §4 module boundary rule; docs/backlog
 * L10-809).
 */
public interface SubscriptionAccessChecker {
    boolean hasActiveAccess(UUID userId);
}

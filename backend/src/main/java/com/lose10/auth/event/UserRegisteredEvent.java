package com.lose10.auth.event;

import java.util.UUID;

/**
 * Published after a user finishes registering. Other modules (profile, in
 * particular) react to this to initialize their own default rows, rather
 * than auth reaching into their repositories directly (docs/ARCHITECTURE.md
 * §4 module boundary rule).
 */
public record UserRegisteredEvent(UUID userId) {
}

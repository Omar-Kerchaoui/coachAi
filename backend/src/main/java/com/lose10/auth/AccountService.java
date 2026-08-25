package com.lose10.auth;

import java.util.UUID;

/**
 * The narrow write interface other modules use to trigger account-level
 * actions owned by auth, without depending on {@link AuthService} directly.
 */
public interface AccountService {
    void deleteAccount(UUID userId);
}

package com.lose10.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserListItem(UUID id, String email, String phoneE164, String status, Instant createdAt) {
}

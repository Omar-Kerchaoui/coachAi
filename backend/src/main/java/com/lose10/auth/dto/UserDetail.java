package com.lose10.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserDetail(UUID id, String email, String phoneE164, String status, boolean emailVerified, Instant createdAt) {
}

package com.lose10.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirm(
        @NotBlank String token,
        @NotBlank @Size(min = 10, max = 128, message = "must be at least 10 characters") String newPassword
) {
}

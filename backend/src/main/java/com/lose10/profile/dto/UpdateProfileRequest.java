package com.lose10.profile.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Past LocalDate dateOfBirth,
        String sex,
        @DecimalMin("50.0") @DecimalMax("260.0") BigDecimal heightCm,
        @Pattern(regexp = "^[A-Z]{2}$", message = "must be an ISO 3166-1 alpha-2 code") String country,
        String timezone
) {
}

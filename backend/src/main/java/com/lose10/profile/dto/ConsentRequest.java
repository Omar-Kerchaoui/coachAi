package com.lose10.profile.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsentRequest(
        @NotBlank String consentType,
        @NotBlank String version,
        String source
) {
}

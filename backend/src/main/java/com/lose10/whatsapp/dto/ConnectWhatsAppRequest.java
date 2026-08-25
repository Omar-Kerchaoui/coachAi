package com.lose10.whatsapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConnectWhatsAppRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "must be E.164 format, e.g. +14155551234") String phoneE164
) {
}

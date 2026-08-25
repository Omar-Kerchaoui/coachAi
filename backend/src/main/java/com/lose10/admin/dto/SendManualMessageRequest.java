package com.lose10.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record SendManualMessageRequest(@NotBlank String message) {
}

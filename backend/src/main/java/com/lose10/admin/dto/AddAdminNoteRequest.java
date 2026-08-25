package com.lose10.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AddAdminNoteRequest(@NotBlank String note) {
}

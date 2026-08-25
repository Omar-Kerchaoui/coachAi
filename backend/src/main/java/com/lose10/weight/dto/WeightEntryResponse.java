package com.lose10.weight.dto;

import com.lose10.weight.WeightEntry;
import com.lose10.weight.WeightSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeightEntryResponse(UUID id, BigDecimal weightKg, Instant recordedAt, WeightSource source) {
    public static WeightEntryResponse from(WeightEntry entry) {
        return new WeightEntryResponse(entry.getId(), entry.getWeightKg(), entry.getRecordedAt(), entry.getSource());
    }
}

package com.lose10.subscription.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutSessionRequest(
        @NotNull UUID planId,
        String couponCode
) {
}

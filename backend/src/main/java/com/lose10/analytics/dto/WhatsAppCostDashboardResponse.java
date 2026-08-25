package com.lose10.analytics.dto;

import java.math.BigDecimal;

public record WhatsAppCostDashboardResponse(
        long messagesSent,
        long messagesReceived,
        long templateMessageCount,
        long sessionMessageCount,
        BigDecimal estimatedCostUsd,
        long activeUsers,
        BigDecimal costPerActiveUserUsd
) {
}

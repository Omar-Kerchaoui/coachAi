package com.lose10.analytics;

import com.lose10.analytics.dto.WhatsAppCostDashboardResponse;
import com.lose10.conversation.MessageDirection;
import com.lose10.whatsapp.WhatsAppMessage;
import com.lose10.whatsapp.WhatsAppMessageType;
import com.lose10.whatsapp.WhatsAppUsageQueryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class WhatsAppCostDashboardService {

    private final WhatsAppUsageQueryService usageQueryService;
    private final WhatsAppCostEstimator costEstimator;
    private final AnalyticsEventRepository analyticsEventRepository;

    public WhatsAppCostDashboardService(WhatsAppUsageQueryService usageQueryService, WhatsAppCostEstimator costEstimator, AnalyticsEventRepository analyticsEventRepository) {
        this.usageQueryService = usageQueryService;
        this.costEstimator = costEstimator;
        this.analyticsEventRepository = analyticsEventRepository;
    }

    public WhatsAppCostDashboardResponse computeFor(Instant from, Instant to) {
        List<WhatsAppMessage> messages = usageQueryService.between(from, to);

        long sent = messages.stream().filter(m -> m.getDirection() == MessageDirection.OUTBOUND).count();
        long received = messages.stream().filter(m -> m.getDirection() == MessageDirection.INBOUND).count();
        long templateCount = messages.stream()
                .filter(m -> m.getDirection() == MessageDirection.OUTBOUND && m.getMessageType() == WhatsAppMessageType.TEMPLATE)
                .count();
        long sessionCount = sent - templateCount;

        BigDecimal totalCost = messages.stream()
                .filter(m -> m.getDirection() == MessageDirection.OUTBOUND)
                .map(m -> costEstimator.costFor(m.getMessageType()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeUsers = analyticsEventRepository.countDistinctUsersByEventTypeAndCreatedAtBetween(AnalyticsEventType.WHATSAPP_MESSAGE_SENT, from, to);
        BigDecimal costPerActiveUser = activeUsers == 0
                ? BigDecimal.ZERO
                : totalCost.divide(BigDecimal.valueOf(activeUsers), 6, RoundingMode.HALF_UP);

        return new WhatsAppCostDashboardResponse(sent, received, templateCount, sessionCount, totalCost, activeUsers, costPerActiveUser);
    }
}

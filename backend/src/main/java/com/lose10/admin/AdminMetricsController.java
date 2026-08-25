package com.lose10.admin;

import com.lose10.analytics.AiCostDashboardService;
import com.lose10.analytics.BusinessMetricsQueryService;
import com.lose10.analytics.WhatsAppCostDashboardService;
import com.lose10.analytics.dto.AiCostDashboardResponse;
import com.lose10.analytics.dto.BusinessMetricsResponse;
import com.lose10.analytics.dto.WhatsAppCostDashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {

    private final BusinessMetricsQueryService businessMetricsQueryService;
    private final AiCostDashboardService aiCostDashboardService;
    private final WhatsAppCostDashboardService whatsAppCostDashboardService;

    public AdminMetricsController(
            BusinessMetricsQueryService businessMetricsQueryService,
            AiCostDashboardService aiCostDashboardService,
            WhatsAppCostDashboardService whatsAppCostDashboardService
    ) {
        this.businessMetricsQueryService = businessMetricsQueryService;
        this.aiCostDashboardService = aiCostDashboardService;
        this.whatsAppCostDashboardService = whatsAppCostDashboardService;
    }

    @GetMapping("/business")
    public BusinessMetricsResponse business() {
        return businessMetricsQueryService.latest();
    }

    @GetMapping("/ai-cost")
    public AiCostDashboardResponse aiCost(@RequestParam(defaultValue = "30") int days) {
        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);
        return aiCostDashboardService.computeFor(from, to);
    }

    @GetMapping("/whatsapp-cost")
    public WhatsAppCostDashboardResponse whatsAppCost(@RequestParam(defaultValue = "30") int days) {
        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);
        return whatsAppCostDashboardService.computeFor(from, to);
    }
}

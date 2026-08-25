package com.lose10.ai;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row per {@link AIProvider#generate} call, written unconditionally by
 * {@link CostTrackingAIProvider} — the primary input to the AI cost
 * dashboard (Epic 9) and the backstop that makes per-user AI rate limiting
 * possible (docs/BUSINESS_LOGIC.md §5).
 */
@Entity
@Table(name = "ai_requests")
public class AiRequestLog extends BaseEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "request_type", nullable = false, length = 30)
    private String requestType;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(name = "estimated_cost_usd", nullable = false)
    private BigDecimal estimatedCostUsd;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false)
    private boolean succeeded;

    protected AiRequestLog() {
    }

    public AiRequestLog(UUID userId, String provider, String model, String requestType,
                         int inputTokens, int outputTokens, BigDecimal estimatedCostUsd,
                         Integer latencyMs, boolean succeeded) {
        this.userId = userId;
        this.provider = provider;
        this.model = model;
        this.requestType = requestType;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.estimatedCostUsd = estimatedCostUsd;
        this.latencyMs = latencyMs;
        this.succeeded = succeeded;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getModel() {
        return model;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public BigDecimal getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}

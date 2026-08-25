package com.lose10.ai;

import com.lose10.ai.claude.ClaudeAIProvider;
import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;
import com.lose10.platform.security.CurrentUser;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The bean everywhere else in the app actually injects as {@link AIProvider}
 * — wraps the raw {@link ClaudeAIProvider} so that every single call,
 * success or failure, produces exactly one {@link AiRequestLog} row. This
 * is enforced structurally (the wrapper, not each call site, owns logging)
 * so it can't be forgotten per call site (docs/AI_ARCHITECTURE.md §9,
 * docs/BUSINESS_LOGIC.md §5).
 */
@Service
@Primary
public class CostTrackingAIProvider implements AIProvider {

    private static final String PROVIDER_NAME = "claude";

    private final ClaudeAIProvider delegate;
    private final CostEstimator costEstimator;
    private final AiRequestLogRepository logRepository;

    public CostTrackingAIProvider(ClaudeAIProvider delegate, CostEstimator costEstimator, AiRequestLogRepository logRepository) {
        this.delegate = delegate;
        this.costEstimator = costEstimator;
        this.logRepository = logRepository;
    }

    @Override
    public AIResponse generate(AIRequest request) {
        UUID userId = CurrentUser.idOrNull();
        long start = System.currentTimeMillis();
        try {
            AIResponse response = delegate.generate(request);
            log(userId, request, response.model(), response.inputTokens(), response.outputTokens(), start, true);
            return response;
        } catch (AIProviderException e) {
            log(userId, request, "unknown", 0, 0, start, false);
            throw e;
        }
    }

    private void log(UUID userId, AIRequest request, String model, int inputTokens, int outputTokens, long startMillis, boolean succeeded) {
        BigDecimal cost = costEstimator.estimate(model, inputTokens, outputTokens);
        int latencyMs = (int) (System.currentTimeMillis() - startMillis);
        logRepository.save(new AiRequestLog(
                userId, PROVIDER_NAME, model, request.purpose().name(),
                inputTokens, outputTokens, cost, latencyMs, succeeded
        ));
    }
}

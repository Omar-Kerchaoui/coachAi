package com.lose10.ai.tools;

import com.lose10.platform.error.ApiException;
import com.lose10.weight.WeightService;
import com.lose10.weight.dto.LogWeightRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reuses {@link WeightService#logWeight}, including its implausible-delta
 * check — the same business rule applies whether the request came from the
 * REST API or the model (docs/backlog L10-613).
 */
@Component
public class LogWeightTool implements AiTool {

    private final WeightService weightService;

    public LogWeightTool(WeightService weightService) {
        this.weightService = weightService;
    }

    @Override
    public String name() {
        return "logWeight";
    }

    @Override
    public String description() {
        return "Logs a new weight entry for the user. If the value is implausibly different from their last "
                + "logged weight, this will return an error asking for confirmation rather than silently accepting it.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "weightKg", Map.of("type", "number"),
                        "confirmImplausible", Map.of("type", "boolean", "description", "set true only if the user explicitly confirmed an unusual value")
                ),
                "required", List.of("weightKg")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            BigDecimal weightKg = BigDecimal.valueOf(((Number) arguments.get("weightKg")).doubleValue());
            boolean confirm = Boolean.TRUE.equals(arguments.get("confirmImplausible"));
            weightService.logWeight(userId, new LogWeightRequest(weightKg, null, confirm));
            return ToolResult.ok("logged");
        } catch (ApiException e) {
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Failed to log weight");
        }
    }
}

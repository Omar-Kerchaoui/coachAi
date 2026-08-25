package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.weight.WeightService;
import com.lose10.weight.dto.WeightEntryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetWeightHistoryTool implements AiTool {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 90;

    private final WeightService weightService;
    private final ObjectMapper objectMapper;

    public GetWeightHistoryTool(WeightService weightService, ObjectMapper objectMapper) {
        this.weightService = weightService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getWeightHistory";
    }

    @Override
    public String description() {
        return "Returns the user's recent weight entries. Optionally accepts 'days' (how many days back to look, default 30, max 90).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of("days", Map.of("type", "integer", "description", "How many days of history to return (max 90)")),
                "required", List.of()
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            int days = extractDays(arguments);
            Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
            List<WeightEntryResponse> entries = weightService.history(userId, from, Instant.now(), PageRequest.of(0, 100)).getContent();
            return ToolResult.ok(objectMapper.writeValueAsString(entries));
        } catch (Exception e) {
            return ToolResult.error("Failed to read weight history");
        }
    }

    private int extractDays(Map<String, Object> arguments) {
        Object raw = arguments.get("days");
        if (raw instanceof Number number) {
            return Math.min(Math.max(number.intValue(), 1), MAX_DAYS);
        }
        return DEFAULT_DAYS;
    }
}

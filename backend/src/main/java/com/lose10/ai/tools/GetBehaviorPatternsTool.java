package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.behavior.BehaviorPattern;
import com.lose10.behavior.BehaviorPatternRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetBehaviorPatternsTool implements AiTool {

    private final BehaviorPatternRepository repository;
    private final ObjectMapper objectMapper;

    public GetBehaviorPatternsTool(BehaviorPatternRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getBehaviorPatterns";
    }

    @Override
    public String description() {
        return "Returns behavioral patterns the system has detected for this user (e.g. recurring missed Friday workouts).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            List<BehaviorPattern> patterns = repository.findByUserIdAndStatus(userId, "ACTIVE");
            List<Map<String, Object>> simplified = patterns.stream()
                    .map(p -> Map.<String, Object>of("description", p.getDescription(), "confidence", p.getConfidence()))
                    .toList();
            return ToolResult.ok(objectMapper.writeValueAsString(simplified));
        } catch (Exception e) {
            return ToolResult.error("Failed to read behavior patterns");
        }
    }
}

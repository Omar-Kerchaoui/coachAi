package com.lose10.ai.tools;

import com.lose10.checkin.CheckInService;
import com.lose10.checkin.CheckInType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Used when the check-in conversation happens right there in the chat
 * (the AI asks, the user answers, the AI records it) — creates and
 * completes the check-in in one step, rather than requiring a
 * separately-scheduled check-in to already exist.
 */
@Component
public class CreateCheckInTool implements AiTool {

    private final CheckInService checkInService;

    public CreateCheckInTool(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @Override
    public String name() {
        return "createCheckIn";
    }

    @Override
    public String description() {
        return "Records a check-in based on what the user just told you in conversation (mood, energy, adherence, or a weekly summary).";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "type", Map.of("type", "string", "enum", List.of("MORNING", "EVENING", "WEEKLY")),
                        "mood", Map.of("type", "string"),
                        "energy", Map.of("type", "string"),
                        "adherence", Map.of("type", "string")
                ),
                "required", List.of("type")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            CheckInType type = CheckInType.valueOf((String) arguments.get("type"));
            var checkIn = checkInService.create(userId, type, Instant.now());
            checkInService.complete(userId, checkIn.getId(), (Map<String, Object>) (Map<?, ?>) arguments);
            return ToolResult.ok("recorded");
        } catch (Exception e) {
            return ToolResult.error("Failed to record check-in");
        }
    }
}

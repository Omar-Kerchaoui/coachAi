package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.checkin.CheckInService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GetRecentCheckInsTool implements AiTool {

    private final CheckInService checkInService;
    private final ObjectMapper objectMapper;

    public GetRecentCheckInsTool(CheckInService checkInService, ObjectMapper objectMapper) {
        this.checkInService = checkInService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "getRecentCheckIns";
    }

    @Override
    public String description() {
        return "Returns the user's recent check-ins (morning/evening/weekly) and their responses.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(checkInService.recent(userId)));
        } catch (Exception e) {
            return ToolResult.error("Failed to read recent check-ins");
        }
    }
}

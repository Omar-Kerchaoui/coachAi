package com.lose10.ai.tools;

import com.lose10.memory.MemoryCategory;
import com.lose10.memory.MemoryImportance;
import com.lose10.memory.MemoryWriteService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Distinct tool name from {@link SaveMemoryTool} for the model's benefit
 * (clearer intent when it already knows a memory exists and is revising
 * it), but the same underlying upsert path — see
 * {@link MemoryWriteService#upsert}.
 */
@Component
public class UpdateMemoryTool implements AiTool {

    private final MemoryWriteService memoryWriteService;

    public UpdateMemoryTool(MemoryWriteService memoryWriteService) {
        this.memoryWriteService = memoryWriteService;
    }

    @Override
    public String name() {
        return "updateMemory";
    }

    @Override
    public String description() {
        return "Updates an existing memory (identified by category + key) with new content — e.g. the user's "
                + "preferred training time changed. Creates the memory if it doesn't already exist.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "category", Map.of("type", "string", "enum", java.util.Arrays.stream(MemoryCategory.values()).map(Enum::name).toList()),
                        "key", Map.of("type", "string"),
                        "content", Map.of("type", "string"),
                        "importance", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")),
                        "confidence", Map.of("type", "number")
                ),
                "required", List.of("category", "key", "content", "importance", "confidence")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            memoryWriteService.upsert(
                    userId,
                    MemoryCategory.valueOf((String) arguments.get("category")),
                    (String) arguments.get("key"),
                    (String) arguments.get("content"),
                    MemoryImportance.valueOf((String) arguments.get("importance")),
                    BigDecimal.valueOf(((Number) arguments.get("confidence")).doubleValue()),
                    null
            );
            return ToolResult.ok("updated");
        } catch (Exception e) {
            return ToolResult.error("Failed to update memory: " + e.getMessage());
        }
    }
}

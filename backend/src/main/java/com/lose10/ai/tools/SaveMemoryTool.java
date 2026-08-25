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
 * Shares {@link MemoryWriteService#upsert} with the async extraction path
 * and {@link UpdateMemoryTool} — one dedup/contradiction rule set
 * regardless of entry point (docs/AI_ARCHITECTURE.md §4).
 */
@Component
public class SaveMemoryTool implements AiTool {

    private final MemoryWriteService memoryWriteService;

    public SaveMemoryTool(MemoryWriteService memoryWriteService) {
        this.memoryWriteService = memoryWriteService;
    }

    @Override
    public String name() {
        return "saveMemory";
    }

    @Override
    public String description() {
        return "Saves a durable fact, preference, or pattern about the user for future conversations. "
                + "Only use for information worth remembering long-term, not passing conversational state.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "category", Map.of("type", "string", "enum", categoryNames()),
                        "key", Map.of("type", "string", "description", "short snake_case identifier, e.g. 'friday_night_overeating'"),
                        "content", Map.of("type", "string", "description", "concise factual statement in third person"),
                        "importance", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL")),
                        "confidence", Map.of("type", "number", "description", "0.0-1.0")
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
            return ToolResult.ok("saved");
        } catch (Exception e) {
            return ToolResult.error("Failed to save memory: " + e.getMessage());
        }
    }

    private List<String> categoryNames() {
        return java.util.Arrays.stream(MemoryCategory.values()).map(Enum::name).toList();
    }
}

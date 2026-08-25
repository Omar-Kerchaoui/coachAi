package com.lose10.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.memory.MemoryCategory;
import com.lose10.memory.MemoryRecord;
import com.lose10.memory.MemoryRetriever;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SearchMemoriesTool implements AiTool {

    private final MemoryRetriever memoryRetriever;
    private final ObjectMapper objectMapper;

    public SearchMemoriesTool(MemoryRetriever memoryRetriever, ObjectMapper objectMapper) {
        this.memoryRetriever = memoryRetriever;
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "searchMemories";
    }

    @Override
    public String description() {
        return "Semantically searches the user's own long-term memories for anything relevant to a topic. "
                + "Use when you need more context than what's already in the conversation.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of("type", "string", "description", "what to search for"),
                        "category", Map.of("type", "string", "enum", java.util.Arrays.stream(MemoryCategory.values()).map(Enum::name).toList())
                ),
                "required", List.of("query")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            String query = (String) arguments.get("query");
            MemoryCategory category = arguments.containsKey("category")
                    ? MemoryCategory.valueOf((String) arguments.get("category"))
                    : null;
            List<MemoryRecord> results = memoryRetriever.retrieve(userId, query, category);
            List<Map<String, Object>> simplified = results.stream()
                    .map(m -> Map.<String, Object>of("category", m.getCategory().name(), "content", m.getContent()))
                    .toList();
            return ToolResult.ok(objectMapper.writeValueAsString(simplified));
        } catch (Exception e) {
            return ToolResult.error("Memory search failed");
        }
    }
}

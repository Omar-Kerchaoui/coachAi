package com.lose10.ai.tools;

/**
 * Always structured/plain data, never natural language phrased as an
 * instruction — a defense against second-order prompt injection via tool
 * output (docs/AI_ARCHITECTURE.md §4, §6).
 */
public record ToolResult(String content, boolean isError) {
    public static ToolResult ok(String content) {
        return new ToolResult(content, false);
    }

    public static ToolResult error(String message) {
        return new ToolResult(message, true);
    }
}

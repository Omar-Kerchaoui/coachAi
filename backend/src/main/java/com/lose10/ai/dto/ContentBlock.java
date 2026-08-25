package com.lose10.ai.dto;

import java.util.Map;

/**
 * Mirrors Anthropic's Messages API content block shapes closely enough
 * that {@code ClaudeAIProvider} maps to/from real request JSON with no
 * translation layer in between — the rest of the codebase never needs to
 * know it's talking to Claude specifically (docs/AI_ARCHITECTURE.md §1).
 */
public sealed interface ContentBlock {

    record TextBlock(String text) implements ContentBlock {
    }

    record ToolUseBlock(String id, String name, Map<String, Object> input) implements ContentBlock {
    }

    /**
     * {@code content} is always structured data (see docs/AI_ARCHITECTURE.md
     * §4, §6) — never a natural-language instruction — specifically to
     * block second-order prompt injection via tool output.
     */
    record ToolResultBlock(String toolUseId, String content, boolean isError) implements ContentBlock {
    }
}

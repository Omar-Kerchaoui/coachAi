package com.lose10.ai.dto;

import java.util.List;

public record AIResponse(
        List<ContentBlock> content,
        StopReason stopReason,
        int inputTokens,
        int outputTokens,
        String model
) {
    public enum StopReason {
        END_TURN, TOOL_USE, MAX_TOKENS
    }

    public List<ContentBlock.ToolUseBlock> toolUses() {
        return content.stream()
                .filter(ContentBlock.ToolUseBlock.class::isInstance)
                .map(ContentBlock.ToolUseBlock.class::cast)
                .toList();
    }

    public String text() {
        return content.stream()
                .filter(ContentBlock.TextBlock.class::isInstance)
                .map(ContentBlock.TextBlock.class::cast)
                .map(ContentBlock.TextBlock::text)
                .reduce("", (a, b) -> a + b);
    }
}

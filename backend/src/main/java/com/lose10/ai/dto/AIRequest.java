package com.lose10.ai.dto;

import java.util.List;

public record AIRequest(
        String systemPrompt,
        List<Message> messages,
        List<ToolDefinition> tools,
        int maxTokens,
        AIRequestPurpose purpose
) {
    /**
     * Distinguishes low-stakes sub-tasks (e.g. intent classification) from
     * the primary conversational turn, so model selection can route cheaper
     * models to the former (docs/AI_ARCHITECTURE.md §9 cost control).
     */
    public enum AIRequestPurpose {
        COACHING_TURN, INTENT_CLASSIFICATION, SAFETY_CLASSIFICATION, MEMORY_EXTRACTION
    }
}

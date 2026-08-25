package com.lose10.ai.tools;

import java.util.Map;
import java.util.UUID;

/**
 * A tool the model can invoke. {@code userId} is always supplied by
 * {@link ToolRegistry} from the orchestrator's own trusted identity
 * resolution — never from {@code arguments}, which is exactly the
 * model-controlled input Claude produced. An implementation must never look
 * for a user id inside {@code arguments}; doing so would let a
 * prompt-injected or hallucinated id override the real caller
 * (docs/AI_ARCHITECTURE.md §4, docs/SECURITY.md §3).
 */
public interface AiTool {

    String name();

    String description();

    /**
     * JSON Schema object describing the tool's input, in the shape the
     * Messages API expects directly (see docs/AI_ARCHITECTURE.md §4).
     */
    Map<String, Object> inputSchema();

    ToolResult execute(UUID userId, Map<String, Object> arguments);
}

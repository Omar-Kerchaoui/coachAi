package com.lose10.ai;

import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;

/**
 * The only abstraction the rest of the codebase talks to for text
 * generation — no other module may reference the Claude SDK/HTTP client
 * directly (docs/AI_ARCHITECTURE.md §1), enforced by
 * {@link ArchitectureBoundaryTest}.
 */
public interface AIProvider {

    /**
     * @throws AIProviderException on timeout, transport failure, or exhausted
     *                             retries — callers (the orchestrator) decide the
     *                             user-facing fallback; this layer never invents one.
     */
    AIResponse generate(AIRequest request);
}

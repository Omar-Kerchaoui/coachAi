package com.lose10.ai;

/**
 * Thrown after retries are exhausted or on a non-retryable transport error.
 * Never propagated to the client as-is — the orchestrator catches this and
 * substitutes a safe scripted fallback (docs/AI_ARCHITECTURE.md §5).
 */
public class AIProviderException extends RuntimeException {
    public AIProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AIProviderException(String message) {
        super(message);
    }
}

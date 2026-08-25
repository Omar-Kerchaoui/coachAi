package com.lose10.ai;

import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;
import com.lose10.ai.dto.ContentBlock;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Deterministic test double standing in for real Claude calls — the
 * orchestration layer's tests are fixture-based, never hitting the real
 * API (docs/TESTING.md §1 "AI orchestration" row).
 */
public class FakeAIProvider implements AIProvider {

    // LinkedList, not ArrayDeque: queueFailure() needs to enqueue a null
    // sentinel, and ArrayDeque throws NullPointerException on add(null).
    private final Deque<AIResponse> queuedResponses = new LinkedList<>();
    private AIRequest lastRequest;

    public FakeAIProvider queueTextResponse(String text) {
        queuedResponses.add(new AIResponse(
                List.of(new ContentBlock.TextBlock(text)),
                AIResponse.StopReason.END_TURN,
                10, 5, "fake-model"
        ));
        return this;
    }

    public FakeAIProvider queueToolUseResponse(String toolName, String toolUseId, java.util.Map<String, Object> input) {
        queuedResponses.add(new AIResponse(
                List.of(new ContentBlock.ToolUseBlock(toolUseId, toolName, input)),
                AIResponse.StopReason.TOOL_USE,
                10, 5, "fake-model"
        ));
        return this;
    }

    public FakeAIProvider queueFailure() {
        queuedResponses.add(null);
        return this;
    }

    @Override
    public AIResponse generate(AIRequest request) {
        this.lastRequest = request;
        if (queuedResponses.isEmpty()) {
            throw new IllegalStateException("No queued response for FakeAIProvider — test bug");
        }
        AIResponse next = queuedResponses.poll();
        if (next == null) {
            throw new AIProviderException("Simulated provider failure");
        }
        return next;
    }

    public AIRequest lastRequest() {
        return lastRequest;
    }
}

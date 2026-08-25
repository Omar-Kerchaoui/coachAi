package com.lose10.ai.orchestrator;

import com.lose10.ai.AIProvider;
import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;
import com.lose10.ai.dto.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Classifies an inbound message so the orchestrator knows which context
 * sections and tools are relevant (docs/AI_ARCHITECTURE.md §2). Uses a
 * short, cheap classification call — routed to a lower-cost model at the
 * provider/config level (docs/AI_ARCHITECTURE.md §9), not hardcoded here.
 */
@Component
public class IntentDetector {

    private static final String SYSTEM_PROMPT = """
            Classify the user's message into exactly one category. Respond with
            only the category name, nothing else.

            Categories:
            LOG_DATA - reporting a fact to be recorded (weight, a meal, a completed workout)
            QUESTION - asking the coach something
            VENTING - expressing frustration, tiredness, or a struggle without asking anything
            SMALL_TALK - greetings or casual conversation with no coaching content
            SAFETY_CONCERN - anything touching on self-harm, disordered eating, injury, or medical symptoms
            COMMAND - an instruction to change settings (pause coaching, change style, etc.)
            """;

    private final AIProvider aiProvider;

    public IntentDetector(AIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public Intent detect(String message) {
        AIRequest request = new AIRequest(
                SYSTEM_PROMPT,
                List.of(Message.user(message)),
                List.of(),
                10,
                AIRequest.AIRequestPurpose.INTENT_CLASSIFICATION
        );
        AIResponse response = aiProvider.generate(request);
        return parse(response.text());
    }

    private Intent parse(String rawText) {
        String normalized = rawText.strip().toUpperCase();
        for (Intent intent : Intent.values()) {
            if (normalized.contains(intent.name())) {
                return intent;
            }
        }
        return Intent.QUESTION;
    }
}

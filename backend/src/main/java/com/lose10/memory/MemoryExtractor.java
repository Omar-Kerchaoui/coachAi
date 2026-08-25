package com.lose10.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.ai.AIProvider;
import com.lose10.ai.AIProviderException;
import com.lose10.ai.dto.AIRequest;
import com.lose10.ai.dto.AIResponse;
import com.lose10.ai.dto.Message;
import com.lose10.memory.dto.MemoryCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a single user message into zero or one memory writes
 * (docs/MEMORY.md §4). Runs asynchronously, off the request path that
 * produces the coaching reply (docs/backlog L10-507) — callers own that
 * async boundary; this class is just the extraction decision. The actual
 * create-or-update logic lives in {@link MemoryWriteService}, shared with
 * the AI tools.
 */
@Service
public class MemoryExtractor {

    private static final Logger log = LoggerFactory.getLogger(MemoryExtractor.class);

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            You extract durable, memory-worthy facts from a single user message sent to a weight-loss coaching app.

            Only extract something if it describes a RECURRING pattern, an explicit PREFERENCE, a LIFESTYLE fact,
            a stated MOTIVATION, a notable EVENT with a date, or similar durable information — not today's mood,
            today's soreness, or other one-off state. "I'm tired today" is NOT memory-worthy. "I always feel
            exhausted on Mondays because I sleep late on Sundays" IS memory-worthy.

            Respond with ONLY a JSON object, no other text, no markdown fences:
            {"worth_remembering": true, "category": "BEHAVIOR", "key": "short_snake_case_key", "content": "concise factual statement in third person", "importance": "MEDIUM", "confidence": 0.8}

            category must be one of: PROFILE, GOAL, PREFERENCE, BEHAVIOR, LIFESTYLE, MOTIVATION, EVENT, COACHING_PREFERENCE, RELATIONSHIP_CONTEXT, OTHER
            importance must be one of: LOW, MEDIUM, HIGH, CRITICAL
            confidence is 0.0-1.0: how certain you are this is accurate and durable.

            If nothing in the message is memory-worthy, respond with exactly:
            {"worth_remembering": false}
            """;

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);
    private static final double MIN_CONFIDENCE_TO_STORE = 0.5;

    private final AIProvider aiProvider;
    private final MemoryWriteService memoryWriteService;
    private final ObjectMapper objectMapper;

    public MemoryExtractor(AIProvider aiProvider, MemoryWriteService memoryWriteService, ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.memoryWriteService = memoryWriteService;
        this.objectMapper = objectMapper;
    }

    public Optional<MemoryRecord> extractFrom(UUID userId, String userMessage, UUID sourceMessageId) {
        MemoryCandidate candidate = detectCandidate(userId, userMessage);
        if (!candidate.worthRemembering() || candidate.confidence() < MIN_CONFIDENCE_TO_STORE) {
            return Optional.empty();
        }

        MemoryRecord record = memoryWriteService.upsert(
                userId, candidate.category(), candidate.key(), candidate.content(),
                candidate.importance(), toConfidence(candidate.confidence()), sourceMessageId
        );
        return Optional.of(record);
    }

    private MemoryCandidate detectCandidate(UUID userId, String message) {
        try {
            AIResponse response = aiProvider.generate(new AIRequest(
                    EXTRACTION_SYSTEM_PROMPT, List.of(Message.user(message)), List.of(), 300, AIRequest.AIRequestPurpose.MEMORY_EXTRACTION, userId
            ));
            return parseCandidate(response.text());
        } catch (AIProviderException e) {
            log.warn("Memory extraction call failed, skipping this message", e);
            return MemoryCandidate.NONE;
        }
    }

    private MemoryCandidate parseCandidate(String rawText) {
        Matcher matcher = JSON_PATTERN.matcher(rawText);
        if (!matcher.find()) {
            return MemoryCandidate.NONE;
        }
        try {
            return objectMapper.readValue(matcher.group(), MemoryCandidate.class);
        } catch (Exception e) {
            log.warn("Could not parse memory extraction response: {}", rawText);
            return MemoryCandidate.NONE;
        }
    }

    private BigDecimal toConfidence(double confidence) {
        return BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
    }
}

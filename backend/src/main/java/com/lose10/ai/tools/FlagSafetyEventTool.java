package com.lose10.ai.tools;

import com.lose10.ai.safety.SafetyAssessment;
import com.lose10.ai.safety.SafetyCategory;
import com.lose10.ai.safety.SafetySeverity;
import com.lose10.safety.SafetyEventService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lets the model flag something concerning that the heuristic pre-screen
 * (docs/ai/safety/SafetyAnalyzer) didn't catch — e.g. a subtler pattern
 * that only becomes clear over the course of a conversation. Always
 * available regardless of coaching state (docs/AI_ARCHITECTURE.md §4).
 */
@Component
public class FlagSafetyEventTool implements AiTool {

    private final SafetyEventService safetyEventService;

    public FlagSafetyEventTool(SafetyEventService safetyEventService) {
        this.safetyEventService = safetyEventService;
    }

    @Override
    public String name() {
        return "flagSafetyEvent";
    }

    @Override
    public String description() {
        return "Flags a safety concern for human review — use if something concerning comes up that isn't a clear-cut emergency.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "category", Map.of("type", "string", "enum", List.of(
                                "STARVATION_OR_EXTREME_RESTRICTION", "DEHYDRATION", "EATING_DISORDER_INDICATOR",
                                "SELF_HARM", "SEVERE_INJURY_OR_SYMPTOM", "MEDICATION_OR_DIAGNOSIS_REQUEST", "UNSAFE_TRAINING_REQUEST"
                        )),
                        "reason", Map.of("type", "string")
                ),
                "required", List.of("category", "reason")
        );
    }

    @Override
    public ToolResult execute(UUID userId, Map<String, Object> arguments) {
        try {
            SafetyCategory category = SafetyCategory.valueOf((String) arguments.get("category"));
            String reason = (String) arguments.get("reason");
            safetyEventService.record(userId, new SafetyAssessment(category, SafetySeverity.MEDIUM, "AI-flagged"), reason);
            return ToolResult.ok("flagged for review");
        } catch (Exception e) {
            return ToolResult.error("Failed to flag safety event");
        }
    }
}

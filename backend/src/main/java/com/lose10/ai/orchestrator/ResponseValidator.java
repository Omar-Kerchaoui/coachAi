package com.lose10.ai.orchestrator;

import com.lose10.ai.safety.SafetyAnalyzer;
import com.lose10.ai.safety.SafetyAssessment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The pre-send gate every generated response passes through before
 * reaching WhatsApp (docs/AI_ARCHITECTURE.md §5). On any rejection the
 * caller must use a scripted fallback — this class only decides
 * valid/invalid, never composes the fallback itself.
 */
@Component
public class ResponseValidator {

    private static final int MAX_RESPONSE_LENGTH = 2000;

    private static final List<Pattern> LEAK_PATTERNS = List.of(
            Pattern.compile("system prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\byou are (an? )?ai coach named\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<tool_result>|<system>|input_schema", Pattern.CASE_INSENSITIVE)
    );

    private static final Pattern DIAGNOSIS_PATTERN = Pattern.compile(
            "\\byou (have|may have|likely have|probably have) (diabetes|a thyroid (problem|condition)|pcos|an eating disorder)\\b",
            Pattern.CASE_INSENSITIVE);

    private final SafetyAnalyzer safetyAnalyzer;

    public ResponseValidator(SafetyAnalyzer safetyAnalyzer) {
        this.safetyAnalyzer = safetyAnalyzer;
    }

    public ValidationResult validate(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return ValidationResult.reject("empty response");
        }
        if (responseText.length() > MAX_RESPONSE_LENGTH) {
            return ValidationResult.reject("response exceeds max length");
        }
        for (Pattern pattern : LEAK_PATTERNS) {
            if (pattern.matcher(responseText).find()) {
                return ValidationResult.reject("possible internal/system leak");
            }
        }
        if (DIAGNOSIS_PATTERN.matcher(responseText).find()) {
            return ValidationResult.reject("medical diagnosis language");
        }
        SafetyAssessment outputSafety = safetyAnalyzer.screen(responseText);
        if (outputSafety.requiresEscalation()) {
            return ValidationResult.reject("unsafe content in generated response: " + outputSafety.category());
        }
        return ValidationResult.ok();
    }
}

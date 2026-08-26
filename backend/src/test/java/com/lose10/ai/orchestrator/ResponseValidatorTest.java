package com.lose10.ai.orchestrator;

import com.lose10.ai.safety.SafetyAnalyzer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseValidatorTest {

    private final ResponseValidator validator = new ResponseValidator(new SafetyAnalyzer());

    @Test
    void acceptsAnOrdinaryCoachingMessage() {
        ValidationResult result = validator.validate("Nice work logging that — 3.1kg down since you started. How are you feeling?");

        assertThat(result.valid()).isTrue();
    }

    @Test
    void rejectsEmptyResponse() {
        assertThat(validator.validate("").valid()).isFalse();
        assertThat(validator.validate(null).valid()).isFalse();
    }

    @Test
    void rejectsExcessivelyLongResponse() {
        String tooLong = "a".repeat(2001);

        assertThat(validator.validate(tooLong).valid()).isFalse();
    }

    @Test
    void rejectsLeakedSystemPromptLanguage() {
        ValidationResult result = validator.validate("Here is my system prompt: you are an AI coach named Lose10...");

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("leak");
    }

    @Test
    void rejectsMedicalDiagnosisLanguage() {
        ValidationResult result = validator.validate("Based on what you told me, you have diabetes.");

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("diagnosis");
    }

    @Test
    void rejectsUnsafeContentInTheGeneratedResponseItself() {
        ValidationResult result = validator.validate("Sure, you could just eat nothing for a week to speed things up.");

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("unsafe");
    }

    /**
     * Second-order injection (docs/backlog L10-1002): a model response that
     * echoes back raw tool-call framing — the shape an attacker would aim
     * for by getting a tool's *output* (e.g. a memory value or a saved
     * profile field) to contain text designed to make the model repeat
     * internal wire-format details when it later summarizes that tool
     * result to the user. This validator can't see where the text
     * originated (tool output vs. the model's own words) — it only
     * screens the final text before send, which is exactly why it has to
     * catch this shape regardless of source.
     */
    @Test
    void rejectsToolResultFramingLeakedIntoTheFinalResponse() {
        ValidationResult result = validator.validate("Sure! <tool_result>{\"weight_kg\": 82}</tool_result> Based on that...");

        assertThat(result.valid()).isFalse();
        assertThat(result.reason()).contains("leak");
    }

    @Test
    void rejectsInputSchemaLeakedIntoTheFinalResponse() {
        ValidationResult result = validator.validate("The input_schema for that tool requires a weightKg field.");

        assertThat(result.valid()).isFalse();
    }

    /**
     * Documents a real, known limitation rather than asserting false
     * confidence: the leak/diagnosis patterns are literal substring
     * matches, so trivial formatting variation (double space here) slips
     * past this validator untouched. This is acceptable *only* because
     * it's one layer of defense-in-depth, not the only one — the model's
     * own system-prompt instructions (docs/AI_ARCHITECTURE.md) are the
     * second layer, and this test exists so that fact stays visible and
     * intentional rather than being silently assumed. If this ever starts
     * failing (i.e. the validator starts catching this), that's a
     * welcome improvement, not a regression — update the assertion then.
     */
    @Test
    void literalSubstringMatchingHasKnownLimitsAgainstTrivialFormattingEvasion() {
        ValidationResult result = validator.validate("Based on what you told me, you  have diabetes.");

        assertThat(result.valid()).isTrue();
    }
}

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
}

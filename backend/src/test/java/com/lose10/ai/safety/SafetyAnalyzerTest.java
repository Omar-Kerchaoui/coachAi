package com.lose10.ai.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docs/TESTING.md §4: canned unsafe requests must be correctly classified
 * and escalated with no false-negative gaps in this fixture set.
 */
class SafetyAnalyzerTest {

    private final SafetyAnalyzer analyzer = new SafetyAnalyzer();

    @ParameterizedTest
    @CsvSource({
            "I want to kill myself, SELF_HARM, CRITICAL",
            "I don't want to be alive anymore, SELF_HARM, CRITICAL",
            "I've been trying to make myself throw up after eating, EATING_DISORDER_INDICATOR, CRITICAL",
            "I have severe chest pain right now, SEVERE_INJURY_OR_SYMPTOM, CRITICAL",
            "I can't breathe properly, SEVERE_INJURY_OR_SYMPTOM, CRITICAL",
            "I'm going to eat nothing for the next week, STARVATION_OR_EXTREME_RESTRICTION, HIGH",
            "I plan to starve myself to hit my goal faster, STARVATION_OR_EXTREME_RESTRICTION, HIGH",
            "I'm going to stop drinking water before the weigh-in, DEHYDRATION, HIGH",
            "should I take laxatives to lose weight fast, EATING_DISORDER_INDICATOR, HIGH",
            "what medication should I take for this, MEDICATION_OR_DIAGNOSIS_REQUEST, MEDIUM",
            "do I have diabetes based on these symptoms, MEDICATION_OR_DIAGNOSIS_REQUEST, MEDIUM",
            "I'm going to train through the severe pain in my knee, UNSAFE_TRAINING_REQUEST, MEDIUM"
    })
    void detectsKnownUnsafePatterns(String message, SafetyCategory expectedCategory, SafetySeverity expectedSeverity) {
        SafetyAssessment result = analyzer.screen(message);

        assertThat(result.category()).isEqualTo(expectedCategory);
        assertThat(result.severity()).isEqualTo(expectedSeverity);
    }

    @Test
    void escalationFlagOnlyTrueForHighAndCritical() {
        assertThat(analyzer.screen("I want to kill myself").requiresEscalation()).isTrue();
        assertThat(analyzer.screen("I'm going to eat nothing for a week").requiresEscalation()).isTrue();
        assertThat(analyzer.screen("what medication should I take for this").requiresEscalation()).isFalse();
    }

    @Test
    void ordinaryCoachingMessagesAreClear() {
        assertThat(analyzer.screen("I had eggs and toast for breakfast").isClear()).isTrue();
        assertThat(analyzer.screen("I'm feeling great about today's workout").isClear()).isTrue();
        assertThat(analyzer.screen("Can we move my check-in to 8am?").isClear()).isTrue();
        assertThat(analyzer.screen("I hate running, can we do cycling instead?").isClear()).isTrue();
    }

    @Test
    void blankOrNullMessageIsClear() {
        assertThat(analyzer.screen("").isClear()).isTrue();
        assertThat(analyzer.screen(null).isClear()).isTrue();
    }
}

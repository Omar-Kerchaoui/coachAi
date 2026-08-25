package com.lose10.ai.safety;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Runs before generation on every inbound message (docs/AI_ARCHITECTURE.md
 * §7). Deliberately pattern-based rather than solely LLM-judged for the
 * fast path: a deterministic, unit-testable safety net that can't silently
 * regress if a model or prompt changes, catching clear-cut cases cheaply
 * before any AI call is made. This is a starting point, not a clinical
 * tool — the pattern list needs review by someone with relevant clinical/
 * legal expertise before this ships to real users (see docs/SECURITY.md
 * §10), and ambiguous cases are exactly what a future AI-based
 * classification pass (not yet wired in — needs a real API key to
 * validate) is for.
 */
@Component
public class SafetyAnalyzer {

    private record Rule(SafetyCategory category, SafetySeverity severity, Pattern pattern) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule(SafetyCategory.SELF_HARM, SafetySeverity.CRITICAL,
                    Pattern.compile("\\b(kill myself|suicide|end my life|want to die|don'?t want to (be alive|live))\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.EATING_DISORDER_INDICATOR, SafetySeverity.CRITICAL,
                    Pattern.compile("\\b(make myself throw up|purge (my food|after eating)|force myself to vomit)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.SEVERE_INJURY_OR_SYMPTOM, SafetySeverity.CRITICAL,
                    Pattern.compile("\\b(chest pain|can'?t breathe|difficulty breathing|passed out|fainted|severe (chest|abdominal) pain)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.STARVATION_OR_EXTREME_RESTRICTION, SafetySeverity.HIGH,
                    Pattern.compile("\\b(eat (nothing|zero calories)|stop eating (completely|entirely)|not eat(ing)? for (\\d+ )?days|starve myself)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.DEHYDRATION, SafetySeverity.HIGH,
                    Pattern.compile("\\b(stop drinking water|dehydrate myself|cut out water (completely|entirely))\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.EATING_DISORDER_INDICATOR, SafetySeverity.HIGH,
                    Pattern.compile("\\b(laxatives to lose weight|binge and purge|diet pills? to (starve|not eat))\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.MEDICATION_OR_DIAGNOSIS_REQUEST, SafetySeverity.MEDIUM,
                    Pattern.compile("\\b(what medication should i take|do i have (diabetes|a thyroid problem)|diagnose me|is this a symptom of)\\b", Pattern.CASE_INSENSITIVE)),
            new Rule(SafetyCategory.UNSAFE_TRAINING_REQUEST, SafetySeverity.MEDIUM,
                    Pattern.compile("\\b(train through (the )?(severe )?pain|ignore the pain and (keep going|push through)|work out with a (fracture|broken bone))\\b", Pattern.CASE_INSENSITIVE))
    );

    public SafetyAssessment screen(String message) {
        if (message == null || message.isBlank()) {
            return SafetyAssessment.CLEAR;
        }
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(message).find()) {
                return new SafetyAssessment(rule.category(), rule.severity(), rule.pattern().pattern());
            }
        }
        return SafetyAssessment.CLEAR;
    }
}

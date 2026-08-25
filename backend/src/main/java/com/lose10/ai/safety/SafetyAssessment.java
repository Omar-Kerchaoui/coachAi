package com.lose10.ai.safety;

public record SafetyAssessment(SafetyCategory category, SafetySeverity severity, String matchedReason) {

    public static final SafetyAssessment CLEAR = new SafetyAssessment(SafetyCategory.NONE, null, null);

    public boolean requiresEscalation() {
        return severity == SafetySeverity.HIGH || severity == SafetySeverity.CRITICAL;
    }

    public boolean isClear() {
        return category == SafetyCategory.NONE;
    }
}

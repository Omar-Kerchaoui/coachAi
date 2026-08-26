package com.lose10.ai.orchestrator;

public record OrchestratorResult(Status status, String message) {

    public enum Status {
        SUCCESS, SAFETY_ESCALATION, PROVIDER_UNAVAILABLE, VALIDATION_FAILED, COACHING_NOT_AUTOMATIC, RATE_LIMITED
    }

    private static final String CRITICAL_SAFETY_FALLBACK =
            "That sounds serious, and it's more than I can safely help with here. If you're thinking about harming "
                    + "yourself or you're in crisis, please contact a crisis line or emergency services right now. "
                    + "I'll be here when you're ready to talk about your plan.";

    private static final String HIGH_SAFETY_FALLBACK =
            "I want to make sure we keep this safe. What you're describing isn't something I can responsibly help "
                    + "with directly — it's worth talking to a doctor or a qualified professional about this. "
                    + "I'm still here for the rest of your plan whenever you're ready.";

    private static final String PROVIDER_UNAVAILABLE_FALLBACK =
            "Give me a sec — let's pick this back up in a moment.";

    private static final String VALIDATION_FAILED_FALLBACK =
            "Sorry, let's try that again — could you rephrase?";

    private static final String RATE_LIMITED_FALLBACK =
            "We've been chatting a lot in the last hour — let's pick this back up a bit later.";

    public static OrchestratorResult success(String text) {
        return new OrchestratorResult(Status.SUCCESS, text);
    }

    public static OrchestratorResult safetyEscalation(com.lose10.ai.safety.SafetySeverity severity) {
        String message = severity == com.lose10.ai.safety.SafetySeverity.CRITICAL ? CRITICAL_SAFETY_FALLBACK : HIGH_SAFETY_FALLBACK;
        return new OrchestratorResult(Status.SAFETY_ESCALATION, message);
    }

    public static OrchestratorResult providerUnavailable() {
        return new OrchestratorResult(Status.PROVIDER_UNAVAILABLE, PROVIDER_UNAVAILABLE_FALLBACK);
    }

    public static OrchestratorResult validationFailed() {
        return new OrchestratorResult(Status.VALIDATION_FAILED, VALIDATION_FAILED_FALLBACK);
    }

    public static OrchestratorResult rateLimited() {
        return new OrchestratorResult(Status.RATE_LIMITED, RATE_LIMITED_FALLBACK);
    }

    /**
     * Coaching is PAUSED or a human has taken over — the caller (e.g. the
     * WhatsApp webhook handler in Epic 7) should not send this message
     * text to the user; it exists so callers have a non-null result to
     * branch on rather than a special-cased null/exception.
     */
    public static OrchestratorResult coachingNotAutomatic() {
        return new OrchestratorResult(Status.COACHING_NOT_AUTOMATIC, null);
    }
}

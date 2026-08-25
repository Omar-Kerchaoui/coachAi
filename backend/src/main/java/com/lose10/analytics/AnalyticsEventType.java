package com.lose10.analytics;

/**
 * The full event catalog from docs/BUSINESS_LOGIC.md §3. Each is emitted
 * from its real corresponding code path (docs/backlog L10-901) — see the
 * call sites of {@link AnalyticsRecorder#record}.
 */
public enum AnalyticsEventType {
    USER_REGISTERED,
    ONBOARDING_STARTED,
    ONBOARDING_COMPLETED,
    TRIAL_STARTED,
    SUBSCRIPTION_STARTED,
    SUBSCRIPTION_CANCELLED,
    WEIGHT_LOGGED,
    WORKOUT_COMPLETED,
    CHECKIN_COMPLETED,
    AI_MESSAGE_SENT,
    AI_MESSAGE_RECEIVED,
    WHATSAPP_MESSAGE_SENT,
    MEMORY_CREATED,
    MEMORY_UPDATED,
    GOAL_MILESTONE_REACHED,
    USER_REENGAGED,
    SAFETY_EVENT
}

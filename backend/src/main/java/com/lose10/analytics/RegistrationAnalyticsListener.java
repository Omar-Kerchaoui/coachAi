package com.lose10.analytics;

import com.lose10.auth.event.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationAnalyticsListener {

    private final AnalyticsRecorder analyticsRecorder;

    public RegistrationAnalyticsListener(AnalyticsRecorder analyticsRecorder) {
        this.analyticsRecorder = analyticsRecorder;
    }

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        analyticsRecorder.record(AnalyticsEventType.USER_REGISTERED, event.userId());
        // Onboarding's only real step in this domain is the goal wizard —
        // "started" fires here so a drop-off before ever creating a goal is
        // still visible in the funnel, paired with ONBOARDING_COMPLETED in
        // GoalService.createGoal.
        analyticsRecorder.record(AnalyticsEventType.ONBOARDING_STARTED, event.userId());
    }
}

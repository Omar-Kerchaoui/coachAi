package com.lose10.safety;

import com.lose10.ai.safety.SafetyAssessment;
import com.lose10.ai.safety.SafetySeverity;
import com.lose10.analytics.AnalyticsEventType;
import com.lose10.analytics.AnalyticsRecorder;
import com.lose10.platform.error.ApiException;
import com.lose10.platform.notification.EmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SafetyEventService {

    private final SafetyEventRepository repository;
    private final AnalyticsRecorder analyticsRecorder;
    private final EmailSender emailSender;
    private final String oncallEmail;

    public SafetyEventService(
            SafetyEventRepository repository,
            AnalyticsRecorder analyticsRecorder,
            EmailSender emailSender,
            @Value("${lose10.safety.oncall-email:}") String oncallEmail
    ) {
        this.repository = repository;
        this.analyticsRecorder = analyticsRecorder;
        this.emailSender = emailSender;
        this.oncallEmail = oncallEmail;
    }

    @Transactional
    public void record(UUID userId, SafetyAssessment assessment, String sourceText) {
        SafetyEvent event = repository.save(new SafetyEvent(userId, assessment.category(), assessment.severity(), "RULE", sourceText));
        analyticsRecorder.record(AnalyticsEventType.SAFETY_EVENT, userId, Map.of(
                "category", assessment.category().name(), "severity", assessment.severity().name()));

        // A real notification, not just a DB row (docs/backlog L10-909 AC) —
        // goes through the same EmailSender abstraction as every other
        // transactional email, so it actually delivers once SES is wired up
        // (docs/DEPLOYMENT.md) rather than requiring a separate on-call
        // integration to be bolted on later.
        if (assessment.severity() == SafetySeverity.CRITICAL && oncallEmail != null && !oncallEmail.isBlank()) {
            emailSender.send(oncallEmail, "CRITICAL safety event — user " + userId,
                    "Category: " + assessment.category() + "\nSeverity: CRITICAL\nUser: " + userId
                            + "\nEvent ID: " + event.getId() + "\nReview in the admin safety queue immediately.");
        }
    }

    @Transactional(readOnly = true)
    public List<SafetyEvent> unresolvedQueue() {
        return repository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<SafetyEvent> forUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void resolve(UUID eventId, UUID adminId, String resolution) {
        SafetyEvent event = repository.findById(eventId).orElseThrow(() -> ApiException.notFound("Safety event not found"));
        event.resolve(adminId, resolution);
    }
}

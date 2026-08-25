package com.lose10.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * The single entry point every module uses to emit a product analytics
 * event (docs/backlog L10-901). Runs off the request thread and never
 * throws — the same "recording never blocks/fails the triggering action"
 * contract as {@code AuditLogService}, since a dropped analytics event is
 * an acceptable loss but a broken coaching turn is not.
 */
@Service
public class AnalyticsRecorder {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRecorder.class);

    private final AnalyticsEventRepository repository;

    public AnalyticsRecorder(AnalyticsEventRepository repository) {
        this.repository = repository;
    }

    @Async("analyticsExecutor")
    @Transactional
    public void record(AnalyticsEventType eventType, UUID userId, Map<String, Object> metadata) {
        try {
            repository.save(new AnalyticsEvent(userId, eventType, metadata));
        } catch (Exception e) {
            log.error("Failed to record analytics event [type={}, userId={}]", eventType, userId, e);
        }
    }

    public void record(AnalyticsEventType eventType, UUID userId) {
        record(eventType, userId, Map.of());
    }
}

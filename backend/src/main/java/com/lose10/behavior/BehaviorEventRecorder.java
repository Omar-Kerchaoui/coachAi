package com.lose10.behavior;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * The write side other domain modules call when something behaviorally
 * interesting happens (a missed workout, a repeated late-night food log,
 * etc.) — this module only owns turning the raw signal stream into
 * patterns, not producing the signals themselves (docs/MEMORY.md §10).
 */
@Service
public class BehaviorEventRecorder {

    private final BehaviorEventRepository repository;

    public BehaviorEventRecorder(BehaviorEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID userId, String eventType, Map<String, Object> payload) {
        repository.save(new BehaviorEvent(userId, eventType, payload));
    }
}

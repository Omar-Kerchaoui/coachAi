package com.lose10.safety;

import com.lose10.ai.safety.SafetyAssessment;
import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SafetyEventService {

    private final SafetyEventRepository repository;

    public SafetyEventService(SafetyEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(UUID userId, SafetyAssessment assessment, String sourceText) {
        repository.save(new SafetyEvent(userId, assessment.category(), assessment.severity(), "RULE", sourceText));
    }

    @Transactional(readOnly = true)
    public List<SafetyEvent> unresolvedQueue() {
        return repository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    @Transactional
    public void resolve(UUID eventId, UUID adminId, String resolution) {
        SafetyEvent event = repository.findById(eventId).orElseThrow(() -> ApiException.notFound("Safety event not found"));
        event.resolve(adminId, resolution);
    }
}

package com.lose10.coaching;

import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CoachingStateService {

    private final CoachingStateRepository stateRepository;
    private final CoachingEventRepository eventRepository;
    private final AuditLogService auditLogService;

    public CoachingStateService(CoachingStateRepository stateRepository, CoachingEventRepository eventRepository, AuditLogService auditLogService) {
        this.stateRepository = stateRepository;
        this.eventRepository = eventRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CoachingState getOrCreate(UUID userId) {
        return stateRepository.findByUserId(userId).orElseGet(() -> stateRepository.save(new CoachingState(userId)));
    }

    /**
     * Checked at the orchestrator's entry gate before any generation call
     * is made (docs/SECURITY.md §11) — not just before sending the
     * response, which would leave a race window between AI and human both
     * responding to the same inbound message.
     */
    @Transactional(readOnly = true)
    public boolean allowsAutomaticAiResponse(UUID userId) {
        return getOrCreate(userId).allowsAutomaticAiResponse();
    }

    @Transactional
    public CoachingState pause(UUID userId, String reason, UUID actorId) {
        CoachingState state = getOrCreate(userId);
        state.setMode(CoachingMode.PAUSED, reason, actorId);
        eventRepository.save(new CoachingEvent(userId, "PAUSED", Map.of("reason", reason == null ? "" : reason), actorId == null ? "USER" : "ADMIN"));
        auditLogService.record(actorId, AuditLog.ActorType.ADMIN, "AI_PAUSED", "CoachingState", userId);
        return state;
    }

    @Transactional
    public CoachingState resume(UUID userId, UUID actorId) {
        CoachingState state = getOrCreate(userId);
        state.setMode(CoachingMode.AI_ACTIVE, null, actorId);
        eventRepository.save(new CoachingEvent(userId, "RESUMED", Map.of(), actorId == null ? "USER" : "ADMIN"));
        auditLogService.record(actorId, AuditLog.ActorType.ADMIN, "AI_RESUMED", "CoachingState", userId);
        return state;
    }

    @Transactional
    public CoachingState setHumanActive(UUID userId, UUID adminId) {
        CoachingState state = getOrCreate(userId);
        state.setMode(CoachingMode.HUMAN_ACTIVE, null, adminId);
        eventRepository.save(new CoachingEvent(userId, "HUMAN_TAKEOVER", Map.of(), "ADMIN"));
        return state;
    }
}

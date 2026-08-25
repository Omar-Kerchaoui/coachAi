package com.lose10.admin;

import com.lose10.admin.dto.ResolveSafetyEventRequest;
import com.lose10.admin.dto.SafetyEventResponse;
import com.lose10.ai.safety.SafetySeverity;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.platform.error.ApiException;
import com.lose10.platform.security.CurrentUser;
import com.lose10.safety.SafetyEvent;
import com.lose10.safety.SafetyEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/safety-events")
public class AdminSafetyController {

    private final SafetyEventService safetyEventService;
    private final AuditLogService auditLogService;

    public AdminSafetyController(SafetyEventService safetyEventService, AuditLogService auditLogService) {
        this.safetyEventService = safetyEventService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public List<SafetyEventResponse> queue() {
        return safetyEventService.unresolvedQueue().stream().map(SafetyEventResponse::from).collect(Collectors.toList());
    }

    /**
     * CRITICAL events require ADMIN specifically — SUPPORT can triage and
     * resolve everything else (docs/backlog L10-904's "per-action
     * permission checks beyond role membership").
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID id, @Valid @RequestBody ResolveSafetyEventRequest request) {
        List<SafetyEvent> unresolved = safetyEventService.unresolvedQueue();
        SafetyEvent event = unresolved.stream().filter(e -> e.getId().equals(id)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Safety event not found or already resolved"));

        if (event.getSeverity() == SafetySeverity.CRITICAL && !CurrentUser.hasRole("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UUID adminId = CurrentUser.id();
        safetyEventService.resolve(id, adminId, request.resolution());
        auditLogService.record(adminId, AuditLog.ActorType.ADMIN, "SAFETY_EVENT_RESOLVED", "SafetyEvent", id);
        return ResponseEntity.noContent().build();
    }
}

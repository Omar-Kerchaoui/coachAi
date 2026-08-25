package com.lose10.ai;

import com.lose10.ai.dto.SendMessageRequest;
import com.lose10.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authenticated test harness for the coaching pipeline before Epic 7 wires
 * up the real WhatsApp entry point — exercises the exact same
 * {@link CoachingTurnService} path a WhatsApp message will use. Not part of
 * the documented public API surface (docs/API.md); exists purely so the
 * orchestrator/memory/tool wiring can be verified end-to-end against a
 * running backend.
 */
@RestController
@RequestMapping("/api/v1/_internal/coach")
public class AiTestController {

    private final CoachingTurnService coachingTurnService;

    public AiTestController(CoachingTurnService coachingTurnService) {
        this.coachingTurnService = coachingTurnService;
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        String response = coachingTurnService.handleInboundMessage(CurrentUser.id(), request.message());
        // response is null when coaching is PAUSED or HUMAN_ACTIVE (docs/SECURITY.md §11) —
        // Map.of() throws NullPointerException on a null value, so this can't use it directly.
        Map<String, String> body = new java.util.HashMap<>();
        body.put("response", response);
        return ResponseEntity.ok(body);
    }
}

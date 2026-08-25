package com.lose10.coaching;

import com.lose10.coaching.dto.CoachingStateResponse;
import com.lose10.coaching.dto.PauseCoachingRequest;
import com.lose10.platform.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User-facing coaching controls (docs product spec §31: pause/resume
 * coaching). Style changes stay on {@code PATCH /users/me/preferences}
 * (Epic 3) — see {@link CoachingState}'s Javadoc for why.
 */
@RestController
@RequestMapping("/api/v1/coaching")
public class CoachingController {

    private final CoachingStateService coachingStateService;

    public CoachingController(CoachingStateService coachingStateService) {
        this.coachingStateService = coachingStateService;
    }

    @GetMapping("/state")
    public ResponseEntity<CoachingStateResponse> state() {
        return ResponseEntity.ok(CoachingStateResponse.from(coachingStateService.getOrCreate(CurrentUser.id())));
    }

    @PostMapping("/pause")
    public ResponseEntity<CoachingStateResponse> pause(@RequestBody(required = false) PauseCoachingRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(CoachingStateResponse.from(coachingStateService.pause(CurrentUser.id(), reason, null)));
    }

    @PostMapping("/resume")
    public ResponseEntity<CoachingStateResponse> resume() {
        return ResponseEntity.ok(CoachingStateResponse.from(coachingStateService.resume(CurrentUser.id(), null)));
    }
}

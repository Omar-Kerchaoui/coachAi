package com.lose10.checkin;

import com.lose10.checkin.dto.CompleteCheckInRequest;
import com.lose10.platform.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping
    public ResponseEntity<List<CheckIn>> list() {
        return ResponseEntity.ok(checkInService.recent(CurrentUser.id()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<CheckIn> complete(@PathVariable UUID id, @RequestBody CompleteCheckInRequest request) {
        return ResponseEntity.ok(checkInService.complete(CurrentUser.id(), id, request.responses()));
    }

    @GetMapping("/weekly-reviews")
    public ResponseEntity<List<WeeklyReview>> weeklyReviews() {
        return ResponseEntity.ok(checkInService.weeklyReviews(CurrentUser.id()));
    }
}

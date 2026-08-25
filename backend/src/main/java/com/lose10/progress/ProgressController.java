package com.lose10.progress;

import com.lose10.platform.security.CurrentUser;
import com.lose10.progress.dto.ProgressSnapshotResponse;
import com.lose10.progress.dto.ProgressSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ProgressSummaryResponse> summary() {
        return ResponseEntity.ok(progressService.summary(CurrentUser.id()));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<ProgressSnapshotResponse>> snapshots() {
        return ResponseEntity.ok(progressService.snapshots(CurrentUser.id()));
    }
}

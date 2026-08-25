package com.lose10.progress;

import com.lose10.goal.Goal;
import com.lose10.goal.GoalService;
import com.lose10.platform.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Computes one progress snapshot per user-with-an-active-goal, once a
 * night. Idempotent: re-running for the same day updates the existing
 * snapshot for that (user, period_end) rather than creating a duplicate,
 * so a retry or an overlapping run is always safe (docs/backlog L10-309).
 * The actual per-user work lives in {@link ProgressSnapshotService} — kept
 * separate so it's invoked through a real Spring proxy (see that class's
 * Javadoc for why that matters).
 */
@Component
public class ProgressSnapshotJob {

    private static final Logger log = LoggerFactory.getLogger(ProgressSnapshotJob.class);
    private static final String LOCK_KEY = "progress-snapshot-job";

    private final GoalService goalService;
    private final ProgressSnapshotService snapshotService;
    private final DistributedLockService lockService;

    public ProgressSnapshotJob(GoalService goalService, ProgressSnapshotService snapshotService, DistributedLockService lockService) {
        this.goalService = goalService;
        this.snapshotService = snapshotService;
        this.lockService = lockService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void run() {
        if (!lockService.tryLock(LOCK_KEY, Duration.ofMinutes(30))) {
            log.info("Skipping progress snapshot run — another instance holds the lock");
            return;
        }
        try {
            for (Goal goal : goalService.listActiveGoals()) {
                try {
                    snapshotService.computeSnapshotFor(goal.getUserId());
                } catch (Exception e) {
                    log.error("Failed to compute progress snapshot for user {}", goal.getUserId(), e);
                }
            }
        } finally {
            lockService.unlock(LOCK_KEY);
        }
    }
}

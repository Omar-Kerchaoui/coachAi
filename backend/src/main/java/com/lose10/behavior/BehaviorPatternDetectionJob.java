package com.lose10.behavior;

import com.lose10.platform.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class BehaviorPatternDetectionJob {

    private static final Logger log = LoggerFactory.getLogger(BehaviorPatternDetectionJob.class);
    private static final String LOCK_KEY = "behavior-pattern-detection-job";
    private static final int LOOKBACK_DAYS = 42;

    private final BehaviorEventRepository eventRepository;
    private final BehaviorPatternDetectionService detectionService;
    private final DistributedLockService lockService;

    public BehaviorPatternDetectionJob(BehaviorEventRepository eventRepository, BehaviorPatternDetectionService detectionService, DistributedLockService lockService) {
        this.eventRepository = eventRepository;
        this.detectionService = detectionService;
        this.lockService = lockService;
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void run() {
        if (!lockService.tryLock(LOCK_KEY, Duration.ofMinutes(30))) {
            log.info("Skipping behavior pattern detection run — another instance holds the lock");
            return;
        }
        try {
            Instant since = Instant.now().minusSeconds(LOOKBACK_DAYS * 86_400L);
            List<UUID> activeUserIds = eventRepository.findDistinctUserIdsWithEventsSince(since);
            for (UUID userId : activeUserIds) {
                try {
                    detectionService.detectForUser(userId);
                } catch (Exception e) {
                    log.error("Behavior pattern detection failed for user {}", userId, e);
                }
            }
        } finally {
            lockService.unlock(LOCK_KEY);
        }
    }
}

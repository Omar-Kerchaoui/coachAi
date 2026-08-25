package com.lose10.notification;

import com.lose10.platform.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Runs every 15 minutes; the poll window passed to
 * {@link ProactiveSchedulerService} matches that interval so nothing due
 * between runs is missed (docs/backlog L10-710). The distributed lock
 * ensures only one backend instance runs a given tick, and the DB unique
 * constraint on scheduled_messages makes each user's daily slot safe to
 * attempt from multiple overlapping runs regardless.
 */
@Component
public class ProactiveSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(ProactiveSchedulerJob.class);
    private static final String LOCK_KEY = "proactive-scheduler-job";
    private static final int POLL_WINDOW_MINUTES = 15;

    private final NotificationPreferencesRepository preferencesRepository;
    private final ProactiveSchedulerService schedulerService;
    private final DistributedLockService lockService;

    public ProactiveSchedulerJob(NotificationPreferencesRepository preferencesRepository, ProactiveSchedulerService schedulerService, DistributedLockService lockService) {
        this.preferencesRepository = preferencesRepository;
        this.schedulerService = schedulerService;
        this.lockService = lockService;
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void run() {
        if (!lockService.tryLock(LOCK_KEY, Duration.ofMinutes(10))) {
            log.debug("Skipping proactive scheduler tick — another instance holds the lock");
            return;
        }
        try {
            List<UUID> userIds = preferencesRepository.findAll().stream().map(NotificationPreferences::getUserId).collect(Collectors.toList());
            for (UUID userId : userIds) {
                try {
                    schedulerService.checkMorningCheckinForUser(userId, POLL_WINDOW_MINUTES);
                } catch (Exception e) {
                    log.error("Proactive scheduler failed for user {}", userId, e);
                }
            }
        } finally {
            lockService.unlock(LOCK_KEY);
        }
    }
}

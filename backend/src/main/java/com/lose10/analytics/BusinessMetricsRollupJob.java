package com.lose10.analytics;

import com.lose10.platform.lock.DistributedLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
public class BusinessMetricsRollupJob {

    private static final Logger log = LoggerFactory.getLogger(BusinessMetricsRollupJob.class);
    private static final String LOCK_KEY = "business-metrics-rollup-job";

    private final BusinessMetricsRollupService rollupService;
    private final DistributedLockService lockService;

    public BusinessMetricsRollupJob(BusinessMetricsRollupService rollupService, DistributedLockService lockService) {
        this.rollupService = rollupService;
        this.lockService = lockService;
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void run() {
        if (!lockService.tryLock(LOCK_KEY, Duration.ofMinutes(30))) {
            log.info("Skipping business metrics rollup — another instance holds the lock");
            return;
        }
        try {
            rollupService.computeFor(LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
        } catch (Exception e) {
            log.error("Business metrics rollup failed", e);
        } finally {
            lockService.unlock(LOCK_KEY);
        }
    }
}

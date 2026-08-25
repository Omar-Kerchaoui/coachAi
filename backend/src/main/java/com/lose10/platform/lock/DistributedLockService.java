package com.lose10.platform.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Simple Redis SETNX-based mutual exclusion for scheduled jobs that must
 * not run concurrently across multiple backend instances — e.g. nightly
 * progress snapshot computation, the proactive coaching scheduler (Epic 7).
 * Not a general-purpose reentrant lock: one attempt, no waiting/blocking.
 */
@Service
public class DistributedLockService {

    private final StringRedisTemplate redis;

    public DistributedLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean tryLock(String key, Duration ttl) {
        Boolean acquired = redis.opsForValue().setIfAbsent("lock:" + key, "1", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    public void unlock(String key) {
        redis.delete("lock:" + key);
    }
}

package com.lose10.platform.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis, shared across all backend
 * instances. Used to enforce the per-endpoint-class limits documented in
 * docs/SECURITY.md §6 (public/user/AI/admin). Not a token bucket — a fixed
 * window is simpler to reason about and sufficient for abuse protection at
 * this stage; revisit only if burst behavior at window boundaries becomes a
 * real problem.
 */
@Service
public class RateLimiterService {

    private final StringRedisTemplate redis;

    public RateLimiterService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * @return true if the call is allowed, false if the limit for this key/window has been exceeded.
     */
    public boolean tryConsume(String key, int limit, Duration window) {
        String redisKey = "ratelimit:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            redis.expire(redisKey, window);
        }
        return count != null && count <= limit;
    }
}

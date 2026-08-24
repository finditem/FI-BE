package com.fmi.domain.post.service;

import com.fmi.domain.post.exception.RateLimitExceededException;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostUpdateRateLimiter {

    private static final int MAX_UPDATES_PER_WINDOW = 5;
    private static final Duration COUNT_WINDOW = Duration.ofMinutes(1);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(3);

    private final StringRedisTemplate stringRedisTemplate;

    public void checkAndRecord(Long postId) {
        String lockKey = lockKey(postId);
        Long lockTtl = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        if (lockTtl != null && lockTtl > 0) {
            throw new RateLimitExceededException(ErrorStatus._POST_UPDATE_RATE_LIMITED, lockTtl);
        }

        String countKey = countKey(postId);
        Long count = stringRedisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(countKey, COUNT_WINDOW);
        }

        if (count != null && count > MAX_UPDATES_PER_WINDOW) {
            stringRedisTemplate.delete(countKey);
            stringRedisTemplate.opsForValue().set(lockKey, "1", LOCK_DURATION);
            throw new RateLimitExceededException(ErrorStatus._POST_UPDATE_RATE_LIMITED, LOCK_DURATION.getSeconds());
        }
    }

    private String lockKey(Long postId) {
        return "post:update:lock:" + postId;
    }

    private String countKey(Long postId) {
        return "post:update:count:" + postId;
    }
}

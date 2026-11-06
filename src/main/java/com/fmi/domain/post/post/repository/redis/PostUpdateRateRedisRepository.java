package com.fmi.domain.post.post.repository.redis;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostUpdateRateRedisRepository {
    private final StringRedisTemplate redis;

    private static final Duration COUNT_WINDOW = Duration.ofMinutes(1);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(3);

    public Long findLockTtl(Long postId) {
        return redis.getExpire("post:update:lock:" + postId, TimeUnit.SECONDS);
    }

    public Long incrementCount(Long postId) {
        String key = "post:update:count:" + postId;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, COUNT_WINDOW);
        }
        return count;
    }

    public void deleteCount(Long postId) {
        redis.delete("post:update:count:" + postId);
    }

    public long lock(Long postId) {
        redis.opsForValue().set("post:update:lock:" + postId, "1", LOCK_DURATION);
        return LOCK_DURATION.getSeconds();
    }
}

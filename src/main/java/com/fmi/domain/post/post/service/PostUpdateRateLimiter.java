package com.fmi.domain.post.post.service;

import com.fmi.domain.post.post.repository.redis.PostUpdateRateRedisRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostUpdateRateLimiter {

    private static final int MAX_UPDATES_PER_WINDOW = 5;

    private final PostUpdateRateRedisRepository rateRedisRepository;

    public void checkAndRecord(Long postId) {
        Long lockTtl = rateRedisRepository.findLockTtl(postId);
        if (lockTtl != null && lockTtl > 0) {
            throw new GeneralException(ErrorStatus._POST_UPDATE_RATE_LIMITED, Map.of("retryAfterSeconds", lockTtl));
        }

        Long count = rateRedisRepository.incrementCount(postId);

        if (count != null && count > MAX_UPDATES_PER_WINDOW) {
            rateRedisRepository.deleteCount(postId);
            long retryAfterSeconds = rateRedisRepository.lock(postId);
            throw new GeneralException(
                    ErrorStatus._POST_UPDATE_RATE_LIMITED, Map.of("retryAfterSeconds", retryAfterSeconds));
        }
    }
}

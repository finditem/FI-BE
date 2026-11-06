package com.fmi.domain.inquiry.inquiry.repository.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InquiryGuestRequestRedisRepository {
    private final StringRedisTemplate redis;

    public Long incrementCount(String ip) {
        String key = "inquiry:guest:count:" + ip;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofMinutes(3));
        }
        return count;
    }

    public void deleteCount(String ip) {
        redis.delete("inquiry:guest:count:" + ip);
    }
}

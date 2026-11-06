package com.fmi.domain.post.post.repository.redis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostViewStore {
    private final StringRedisTemplate stringRedisTemplate;

    public boolean isFirstToday(Long postId, String sic, String value) {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.BASIC_ISO_DATE);
        long ttlSeconds = secondsUntilMidnightSeoul();

        String redisKey = "post:view:" + postId + ":" + today + ":sic:" + sic + ":value:" + value;

        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", Duration.ofSeconds(ttlSeconds));

        return Boolean.TRUE.equals(first);
    }

    private long secondsUntilMidnightSeoul() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).getSeconds();
    }
}

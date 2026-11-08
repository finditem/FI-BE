package com.fmi.external.mail.repository.redis;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmailBounceRedisRepository {
    private static final String BOUNCE_KEY_PREFIX = "email:bounce:";
    private final StringRedisTemplate redis;

    public void register(String email) {
        redis.opsForValue().set(BOUNCE_KEY_PREFIX + email, "true", Duration.ofHours(24));
    }

    public boolean hasBounced(String email) {
        return "true".equals(redis.opsForValue().get(BOUNCE_KEY_PREFIX + email));
    }

    public void delete(String email) {
        redis.delete(BOUNCE_KEY_PREFIX + email);
    }
}

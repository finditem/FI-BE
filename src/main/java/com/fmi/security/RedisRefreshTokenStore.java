package com.fmi.security;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
@Primary
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:jti:";

    private final StringRedisTemplate redis;

    public RedisRefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }

    @Override
    public String issue(String jti, String userEmail, String hashedToken, Instant expiresAt) {
        String k = key(jti);
        HashOperations<String, Object, Object> h = redis.opsForHash();
        h.put(k, "email", userEmail);
        h.put(k, "hash", hashedToken);
        h.put(k, "exp", String.valueOf(expiresAt.getEpochSecond()));
        long ttlSeconds = Math.max(1, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
        redis.expire(k, Duration.ofSeconds(ttlSeconds));
        return jti;
    }

    @Override
    public Optional<TokenMeta> getMeta(String jti) {
        String k = key(jti);
        HashOperations<String, Object, Object> h = redis.opsForHash();
        Object email = h.get(k, "email");
        Object exp = h.get(k, "exp");
        if (email == null || exp == null) return Optional.empty();
        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(exp.toString()));
        return Optional.of(new TokenMeta(email.toString(), expiresAt));
    }

    @Override
    public void revoke(String jti) {
        redis.delete(key(jti));
    }

    @Override
    public void revokeAllForUser(String userEmail) {
        // 간단 구현: 스캔(운영에서는 SCAN 커서 사용 권장). 여기선 안전을 위해 noop로 두거나 별도 인덱스 키 사용 권장.
        // 최소 구현은 생략하고, 필요 시 별도 email->jti 세트 관리 추가.
    }

    @Override
    public boolean validate(String jti, String hashedToken, String userEmail) {
        String k = key(jti);
        HashOperations<String, Object, Object> h = redis.opsForHash();
        Object email = h.get(k, "email");
        Object hash = h.get(k, "hash");
        Object exp = h.get(k, "exp");
        if (email == null || hash == null || exp == null) return false;
        if (!email.equals(userEmail)) return false;
        Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(exp.toString()));
        if (Instant.now().isAfter(expiresAt)) return false;
        return hash.equals(hashedToken);
    }
}



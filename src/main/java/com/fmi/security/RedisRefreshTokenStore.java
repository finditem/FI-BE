package com.fmi.security;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
@Primary
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:jti:";
    private static final String USER_KEY_PREFIX = "refresh:user:";

    private final StringRedisTemplate redis;

    public RedisRefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }

    private String userKey(String userEmail) {
        return USER_KEY_PREFIX + userEmail;
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

        String uk = userKey(userEmail);
        redis.opsForSet().add(uk, jti);
        redis.expire(uk, Duration.ofSeconds(ttlSeconds));

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
        String k = key(jti);
        HashOperations<String, Object, Object> h = redis.opsForHash();
        Object email = h.get(k, "email");
        redis.delete(k);
        if (email != null) {
            redis.opsForSet().remove(userKey(email.toString()), jti);
        }
    }

    @Override
    public void revokeAllForUser(String userEmail) {
        String uk = userKey(userEmail);
        Set<String> jtiSet = redis.opsForSet().members(uk);
        if (jtiSet != null && !jtiSet.isEmpty()) {
            for (String jti : jtiSet) {
                redis.delete(key(jti));
            }
        }
        redis.delete(uk);
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



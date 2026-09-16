package com.fmi.domain.auth.service.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailVerificationStore {

    private static final String CODE_KEY_PREFIX = "email:verify:";
    private static final String VERIFIED_KEY_PREFIX = "email:verified:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFIED_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    public void replaceCode(String email, String code, Instant expiresAt) {
        String key = CODE_KEY_PREFIX + email;
        long expiresAtEpochSecond = expiresAt.getEpochSecond();
        String value = code + ":" + expiresAtEpochSecond;

        redis.delete(key);
        redis.opsForValue().set(key, value, CODE_TTL);
    }

    public Optional<VerificationCode> findCode(String email) {
        String key = CODE_KEY_PREFIX + email;
        String value = redis.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }

        String[] parts = value.split(":");
        if (parts.length != 2) {
            deleteCode(email);
            return Optional.empty();
        }

        try {
            String code = parts[0];
            long expiresAtEpochSecond = Long.parseLong(parts[1]);
            Instant expiresAt = Instant.ofEpochSecond(expiresAtEpochSecond);
            VerificationCode verificationCode = new VerificationCode(code, expiresAt);
            return Optional.of(verificationCode);
        } catch (NumberFormatException exception) {
            deleteCode(email);
            return Optional.empty();
        }
    }

    public void deleteCode(String email) {
        String key = CODE_KEY_PREFIX + email;
        redis.delete(key);
    }

    public void markVerified(String email) {
        String key = VERIFIED_KEY_PREFIX + email;
        redis.opsForValue().set(key, "true", VERIFIED_TTL);
    }

    public boolean isVerified(String email) {
        String key = VERIFIED_KEY_PREFIX + email;
        String value = redis.opsForValue().get(key);
        return "true".equals(value);
    }

    public void consumeVerification(String email) {
        String key = VERIFIED_KEY_PREFIX + email;
        redis.delete(key);
    }

    public record VerificationCode(String value, Instant expiresAt) {}
}

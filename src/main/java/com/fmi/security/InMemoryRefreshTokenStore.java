package com.fmi.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private static class Entry {
        String hashedToken;
        String userEmail;
        Instant expiresAt;
    }

    private final Map<String, Entry> jtiToEntry = new ConcurrentHashMap<>();

    @Override
    public String issue(String jti, String userEmail, String hashedToken, Instant expiresAt) {
        Entry e = new Entry();
        e.hashedToken = hashedToken;
        e.userEmail = userEmail;
        e.expiresAt = expiresAt;
        jtiToEntry.put(jti, e);
        try {
            log.info("[InMemoryRefreshTokenStore] issue jti={} email={} exp={}", jti, userEmail, expiresAt);
        } catch (Exception ignore) {}
        return jti;
    }

    @Override
    public Optional<TokenMeta> getMeta(String jti) {
        Entry e = jtiToEntry.get(jti);
        return e == null ? Optional.empty() : Optional.of(new TokenMeta(e.userEmail, e.expiresAt));
    }

    @Override
    public void revoke(String jti) {
        jtiToEntry.remove(jti);
        try {
            log.info("[InMemoryRefreshTokenStore] revoke jti={}", jti);
        } catch (Exception ignore) {}
    }

    @Override
    public void revokeAllForUser(String userEmail) {
        jtiToEntry.entrySet().removeIf(e -> e.getValue().userEmail.equals(userEmail));
    }

    @Override
    public boolean validate(String jti, String hashedToken, String userEmail) {
        Entry e = jtiToEntry.get(jti);
        if (e == null) return false;
        if (!e.userEmail.equals(userEmail)) return false;
        if (Instant.now().isAfter(e.expiresAt)) return false;
        boolean ok = e.hashedToken.equals(hashedToken);
        try {
            log.info("[InMemoryRefreshTokenStore] validate jti={} email={} result={}", jti, userEmail, ok);
        } catch (Exception ignore) {}
        return ok;
    }
}



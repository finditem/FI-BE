package com.fmi.security;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
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
        return e.hashedToken.equals(hashedToken);
    }
}

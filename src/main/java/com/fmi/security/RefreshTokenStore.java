package com.fmi.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefreshTokenStore {

    private static class Entry {
        String hashedToken;
        String userEmail;
        Instant expiresAt;
        String jti;
    }

    public static class TokenMeta {
        private final String userEmail;
        private final Instant expiresAt;

        public TokenMeta(String userEmail, Instant expiresAt) {
            this.userEmail = userEmail;
            this.expiresAt = expiresAt;
        }

        public String getUserEmail() { return userEmail; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    private final Map<String, Entry> jtiToEntry = new ConcurrentHashMap<>();

    public String issue(String userEmail, String hashedToken, Instant expiresAt) {
        String jti = UUID.randomUUID().toString();
        Entry e = new Entry();
        e.hashedToken = hashedToken;
        e.userEmail = userEmail;
        e.expiresAt = expiresAt;
        e.jti = jti;
        jtiToEntry.put(jti, e);
        return jti;
    }

    public Optional<TokenMeta> getMeta(String jti) {
        Entry e = jtiToEntry.get(jti);
        return e == null ? Optional.empty() : Optional.of(new TokenMeta(e.userEmail, e.expiresAt));
    }

    public void revoke(String jti) {
        jtiToEntry.remove(jti);
    }

    public void revokeAllForUser(String userEmail) {
        jtiToEntry.entrySet().removeIf(e -> e.getValue().userEmail.equals(userEmail));
    }

    public boolean validate(String jti, String hashedToken, String userEmail) {
        Entry e = jtiToEntry.get(jti);
        if (e == null) return false;
        if (!e.userEmail.equals(userEmail)) return false;
        if (Instant.now().isAfter(e.expiresAt)) return false;
        return e.hashedToken.equals(hashedToken);
    }
}



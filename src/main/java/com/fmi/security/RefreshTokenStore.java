package com.fmi.security;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenStore {

    class TokenMeta {
        private final String userEmail;
        private final Instant expiresAt;

        public TokenMeta(String userEmail, Instant expiresAt) {
            this.userEmail = userEmail;
            this.expiresAt = expiresAt;
        }

        public String getUserEmail() { return userEmail; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    String issue(String jti, String userEmail, String hashedToken, Instant expiresAt);

    Optional<TokenMeta> getMeta(String jti);

    void revoke(String jti);

    void revokeAllForUser(String userEmail);

    boolean validate(String jti, String hashedToken, String userEmail);
}



package com.fmi.domain.auth.service;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;
    private final SocialAccountsRepository socialAccountsRepository;

    public IssuedTokens issue(User user, boolean isTemporaryPassword, Provider provider) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        if (provider != null) {
            claims.put("provider", provider.name());
        }
        claims.put("purpose", "access");
        if (isTemporaryPassword) {
            claims.put("isTemporaryPassword", true);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), claims);
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), jti);
        Date accessExpiration = jwtTokenProvider.getExpiration(accessToken);
        Date refreshExpiration = jwtTokenProvider.getExpiration(refreshToken);
        refreshTokenStore.issue(jti, user.getEmail(), sha256Hex(refreshToken), refreshExpiration.toInstant());

        return new IssuedTokens(accessToken, accessExpiration, refreshToken, refreshExpiration);
    }

    public RefreshResult refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return RefreshResult.failure(RefreshFailure.INVALID_TOKEN);
        }

        String email = jwtTokenProvider.getSubject(refreshToken);
        String jti = jwtTokenProvider.getJti(refreshToken);
        if (jti == null || jti.isEmpty()) {
            return RefreshResult.failure(RefreshFailure.MISSING_JTI);
        }

        if (!refreshTokenStore.validate(jti, sha256Hex(refreshToken), email)) {
            return RefreshResult.failure(RefreshFailure.HASH_MISMATCH);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return RefreshResult.failure(RefreshFailure.USER_NOT_FOUND);
        }

        refreshTokenStore.revoke(jti);
        Provider provider = socialAccountsRepository
                .findByUser(user)
                .map(account -> account.getProvider())
                .orElse(null);
        return RefreshResult.success(issue(user, false, provider));
    }

    public void revokeIfValid(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return;
        }

        String jti = jwtTokenProvider.getJti(refreshToken);
        if (jti != null && !jti.isEmpty()) {
            refreshTokenStore.revoke(jti);
        }
    }

    private static String sha256Hex(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    public record IssuedTokens(
            String accessToken, Date accessExpiration, String refreshToken, Date refreshExpiration) {}

    public record RefreshResult(IssuedTokens issuedTokens, RefreshFailure failure) {

        private static RefreshResult success(IssuedTokens issuedTokens) {
            return new RefreshResult(issuedTokens, null);
        }

        private static RefreshResult failure(RefreshFailure failure) {
            return new RefreshResult(null, failure);
        }

        public boolean isSuccess() {
            return issuedTokens != null;
        }
    }

    public enum RefreshFailure {
        INVALID_TOKEN,
        MISSING_JTI,
        HASH_MISMATCH,
        USER_NOT_FOUND
    }
}

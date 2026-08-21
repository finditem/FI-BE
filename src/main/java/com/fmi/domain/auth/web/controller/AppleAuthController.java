package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.domain.auth.service.AppleOAuthService;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.web.dto.AppleLoginRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/apple")
@RequiredArgsConstructor
public class AppleAuthController {

    private final AppleOAuthService appleOAuthService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieFactory cookieFactory;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;

    @PostMapping
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithApple(
            @Valid @RequestBody AppleLoginRequest request, HttpServletRequest httpServletRequest) {
        String subject = appleOAuthService.exchangeCodeForSubject(request.getCode(), request.getEnvironment());
        var localUser = socialLoginService.upsertUserFromApple(subject).user();
        boolean termsAgreed = localUser.isPrivacyPolicyAgreed() && localUser.isTermsOfServiceAgreed();

        var claims = new HashMap<String, Object>();
        claims.put("userId", localUser.getId());
        claims.put("role", localUser.getRole().name());
        claims.put("provider", "APPLE");
        claims.put("purpose", "access");
        String accessToken = jwtTokenProvider.createAccessToken(localUser.getEmail(), claims);

        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(localUser.getEmail(), jti);
        Date refreshExpiration = jwtTokenProvider.getExpiration(refreshToken);
        refreshTokenStore.issue(jti, localUser.getEmail(), sha256Hex(refreshToken), refreshExpiration.toInstant());

        Date accessExpiration = jwtTokenProvider.getExpiration(accessToken);
        ResponseCookie accessCookie = cookieFactory.build(
                httpServletRequest,
                accessCookieName,
                accessToken,
                Duration.between(Instant.now(), accessExpiration.toInstant()));
        ResponseCookie refreshCookie = cookieFactory.build(
                httpServletRequest,
                refreshCookieName,
                refreshToken,
                Duration.between(Instant.now(), refreshExpiration.toInstant()));

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(new LoginResponse(localUser.getId(), false, termsAgreed)));
    }

    private static String sha256Hex(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}

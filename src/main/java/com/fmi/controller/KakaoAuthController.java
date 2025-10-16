package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import com.fmi.service.KakaoOAuthService;
import com.fmi.service.KakaoOAuthService.KakaoToken;
import com.fmi.service.KakaoOAuthService.KakaoUser;
import com.fmi.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
@Tag(name = "Auth-Kakao", description = "카카오 소셜 로그인(토큰 핸드오프)")
public class KakaoAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;
    @Value("${jwt.cookie.secure:false}")
    private boolean refreshCookieSecure;
    @Value("${jwt.cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping
    @Operation(summary = "카카오 로그인(토큰 핸드오프)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithKakao(@RequestBody KakaoLoginRequest req) {
        String grantType = req.getGrantType();
        if (grantType == null) grantType = "authorization_code";

        String kakaoAccessToken;
        if ("authorization_code".equalsIgnoreCase(grantType)) {
            KakaoToken token = kakaoOAuthService.exchangeCodeForToken(req.getCode(), req.getRedirectUri());
            kakaoAccessToken = token.getAccess_token();
        } else if ("access_token".equalsIgnoreCase(grantType)) {
            kakaoAccessToken = req.getAccessToken();
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.onFailure("AUTH400-KAKAO_GRANT", "지원하지 않는 grantType", null));
        }

        KakaoUser user = kakaoOAuthService.getUserInfo(kakaoAccessToken);
        String email = user.getKakao_account() != null ? user.getKakao_account().getEmail() : null;
        String nickname = null;
        String profile = null;
        if (user.getKakao_account() != null && user.getKakao_account().getProfile() != null) {
            nickname = user.getKakao_account().getProfile().getNickname();
            profile = user.getKakao_account().getProfile().getProfile_image_url();
        }
        if (nickname == null && user.getProperties() != null) {
            nickname = user.getProperties().getNickname();
            profile = profile != null ? profile : user.getProperties().getProfile_image();
        }

        var localUser = socialLoginService.upsertUserFromKakao(user.getId(), email, nickname, profile);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", localUser.getId());
        claims.put("provider", "KAKAO");
        String accessToken = jwtTokenProvider.createAccessToken(localUser.getEmail(), claims);

        String jti = UUID.randomUUID().toString();
        String refresh = jwtTokenProvider.createRefreshToken(localUser.getEmail(), jti);
        String refreshHash = sha256Hex(refresh);
        var exp = jwtTokenProvider.getExpiration(refresh).toInstant();
        refreshTokenStore.issue(jti, localUser.getEmail(), refreshHash, exp);

        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refresh)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.between(Instant.now(), exp))
                .build();

        Map<String, Object> result = Map.of("userId", localUser.getId(), "accessToken", accessToken);
        return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(ApiResponse.onSuccess(result));
    }

    private static String sha256Hex(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Data
    @AllArgsConstructor
    public static class KakaoLoginRequest {
        @NotBlank(message = "grantType 필수")
        private String grantType; // authorization_code | access_token
        private String code;
        private String redirectUri;
        private String accessToken;
    }
}



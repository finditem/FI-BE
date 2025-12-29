package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.service.KakaoOAuthService;
import com.fmi.domain.auth.service.KakaoOAuthService.KakaoToken;
import com.fmi.domain.auth.service.KakaoOAuthService.KakaoUser;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.web.dto.KakaoLoginRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth")
public class KakaoAuthController {

    private final KakaoOAuthService kakaoOAuthService;
    private final SocialLoginService socialLoginService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;
    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;
    @Value("${jwt.cookie.secure:false}")
    private boolean refreshCookieSecure;
    @Value("${jwt.cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping
    @Operation(summary = "카카오 로그인")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카카오 로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "AUTH500-KAKAO_TOKEN_FAILED: 카카오 토큰 교환에 실패했습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH500-KAKAO_TOKEN_FAILED\", \"message\": \"카카오 토큰 교환에 실패했습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "AUTH500-KAKAO_USERINFO_FAILED: 카카오 사용자 정보 조회에 실패했습니다",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"AUTH500-KAKAO_USERINFO_FAILED\", \"message\": \"카카오 사용자 정보 조회에 실패했습니다.\"}"
                            )
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "COMMON500: 서버 에러",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"isSuccess\": false, \"code\": \"COMMON500\", \"message\": \"서버 에러, 관리자에게 문의 바랍니다.\"}"
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginWithKakao(@RequestBody KakaoLoginRequest req) {
        KakaoToken token = kakaoOAuthService.exchangeCodeForToken(req.getCode(), req.getRedirectUri());
        String kakaoAccessToken = token.getAccess_token();

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
        var refreshExp = jwtTokenProvider.getExpiration(refresh).toInstant();
        refreshTokenStore.issue(jti, localUser.getEmail(), refreshHash, refreshExp);

        // accessToken 쿠키 설정
        var accessExp = jwtTokenProvider.getExpiration(accessToken).toInstant();
        ResponseCookie accessCookie = ResponseCookie.from(accessCookieName, accessToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.between(Instant.now(), accessExp))
                .build();

        // refreshToken 쿠키 설정
        ResponseCookie refreshCookie = ResponseCookie.from(refreshCookieName, refresh)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(Duration.between(Instant.now(), refreshExp))
                .build();

        Map<String, Object> result = Map.of("userId", localUser.getId());
        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(result));
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
}



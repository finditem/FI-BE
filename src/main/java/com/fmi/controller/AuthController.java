package com.fmi.controller;

import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import com.fmi.service.AuthService;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.fmi.domain.Enum.Role;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "회원가입/로그인 API")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;
    @Value("${jwt.cookie.secure:false}")
    private boolean refreshCookieSecure;
    @Value("${jwt.cookie.same-site:Lax}")
    private String refreshCookieSameSite;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일/비밀번호/닉네임/이름 등을 입력해 회원을 생성합니다. 비밀번호는 8자 이상, 대/소문자·숫자·특수문자를 포함해야 합니다.")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        Long id = authService.signup(
                request.getEmail(),
                request.getPassword(),
                request.getNickname(),
                request.getName(),
                request.getPhoneNumber(),
                request.getProfileImg(),
                request.getRole(),
                request.getTermsOfServiceAgreed(),
                request.getPrivacyPolicyAgreed(),
                request.getMarketingConsent(),
                request.getTrustScore(),
                request.getEmailVerified(),
                request.getPhoneVerified()
        );
        return ApiResponse.onSuccess(new SignupResponse(id));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다. 인증 실패 시 AUTH401-INVALID_CREDENTIALS가 반환됩니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        var user = authService.authenticate(request.getEmail(), request.getPassword());
        var claims = new java.util.HashMap<String, Object>();
        claims.put("userId", user.getUserId());
        claims.put("role", user.getRole().name());
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), claims);
        String jti = java.util.UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail(), jti);

        // refresh token 저장 (해시) 및 쿠키 설정 (쿠키 값은 리프레시 JWT)
        String refreshHash = sha256Hex(refreshToken);
        java.util.Date refreshExp = jwtTokenProvider.getExpiration(refreshToken);
        refreshTokenStore.issue(jti, user.getEmail(), refreshHash, refreshExp.toInstant());

        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.between(java.time.Instant.now(), refreshExp.toInstant()))
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.onSuccess(new LoginResponse(user.getUserId(), accessToken)));
    }

    @Data
    public static class SignupRequest {
        @Schema(description = "이메일", example = "user@example.com")
        @Email
        @NotBlank
        private String email;
        @Schema(description = "비밀번호(규칙 충족)", example = "Abcd1234!")
        @NotBlank
        private String password;
        @Schema(description = "닉네임", example = "johnny")
        @NotBlank
        private String nickname;
        @Schema(description = "이름", example = "John Doe")
        @NotBlank
        private String name;

        // 선택/부가 정보
        @Schema(description = "전화번호", example = "+82-10-1234-5678")
        private String phoneNumber;
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/images/johndoe.png")
        private String profileImg;
        @Schema(description = "역할", example = "USER")
        private Role role; // 기본값 USER (null이면 서버에서 설정)

        // 동의 항목
        @Schema(description = "이용약관 동의", example = "true")
        private Boolean termsOfServiceAgreed;
        @Schema(description = "개인정보 처리방침 동의", example = "true")
        private Boolean privacyPolicyAgreed;
        @Schema(description = "마케팅 수신 동의", example = "false")
        private Boolean marketingConsent;

        // 검증/점수(옵션)
        @Schema(description = "신뢰 점수", example = "75")
        private Long trustScore;
        @Schema(description = "이메일 인증 여부", example = "true")
        private Boolean emailVerified;
        @Schema(description = "전화번호 인증 여부", example = "true")
        private Boolean phoneVerified;
    }

    @Data
    @AllArgsConstructor
    public static class SignupResponse {
        private Long id;
    }

    @Data
    public static class LoginRequest {
        @Schema(description = "이메일", example = "user@example.com")
        @Email
        @NotBlank
        private String email;
        @Schema(description = "비밀번호", example = "Abcd1234!")
        @NotBlank
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private Long userId;
        private String accessToken;
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 리프레시", description = "쿠키의 refresh_token(JWT)으로 액세스 토큰을 갱신합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request,
                                                              @RequestHeader(value = "X-CSRF-Token", required = false) String csrfHeader) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt == null || refreshJwt.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "리프레시 토큰 없음", null));
        }

        if (!jwtTokenProvider.validateToken(refreshJwt)) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시", null));
        }

        String email = jwtTokenProvider.getSubject(refreshJwt);
        String jti = jwtTokenProvider.getJti(refreshJwt);
        if (jti == null || jti.isEmpty()) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시(jti 없음)", null));
        }

        String hash = sha256Hex(refreshJwt);
        if (!refreshTokenStore.validate(jti, hash, email)) {
            return ResponseEntity.status(401).body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "유효하지 않은 리프레시(대조 실패)", null));
        }

        // RTR: 기존 리프레시 폐기, 새 리프레시/쿠키 발급
        refreshTokenStore.revoke(jti);

        var claims = new java.util.HashMap<String, Object>();
        claims.put("purpose", "refresh");
        String accessToken = jwtTokenProvider.createAccessToken(email, claims);

        String newJti = java.util.UUID.randomUUID().toString();
        String newRefresh = jwtTokenProvider.createRefreshToken(email, newJti);
        String newHash = sha256Hex(newRefresh);
        java.util.Date exp = jwtTokenProvider.getExpiration(newRefresh);
        refreshTokenStore.issue(newJti, email, newHash, exp.toInstant());

        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, newRefresh)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(java.time.Duration.between(java.time.Instant.now(), exp.toInstant()))
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(ApiResponse.onSuccess(new LoginResponse(null, accessToken)));
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

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "쿠키의 refresh_token(jti)을 폐기하고 쿠키를 제거합니다.")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt != null && !refreshJwt.isEmpty()) {
            if (jwtTokenProvider.validateToken(refreshJwt)) {
                String jti = jwtTokenProvider.getJti(refreshJwt);
                if (jti != null && !jti.isEmpty()) {
                    refreshTokenStore.revoke(jti);
                }
            }
        }
        ResponseCookie remove = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header("Set-Cookie", remove.toString())
                .body(ApiResponse.onSuccess("OK"));
    }

    private static String getCookieValue(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}



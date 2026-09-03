package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.service.TokenIssuer;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.auth.web.dto.LoginRequest;
import com.fmi.domain.auth.web.dto.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.PasswordVerifyRequest;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.auth.web.response.LoginResponse;
import com.fmi.domain.auth.web.swagger.AuthSwagger;
import com.fmi.domain.user.service.NicknameService;
import com.fmi.domain.user.web.response.CheckResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthSwagger {

    private final AuthService authService;
    private final NicknameService nicknameService;
    private final TokenIssuer tokenIssuer;
    private final PasswordService passwordService;
    private final WithdrawalService withdrawalService;
    private final CookieFactory cookieFactory;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;

    @PostMapping("/auth/signup")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> signup(
            @Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        var user = authService.signup(request);
        return buildTokenResponse(httpRequest, user, false);
    }

    @PostMapping("/auth/login")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        var authResult = authService.authenticate(request.getEmail(), request.getPassword());
        return buildTokenResponse(httpRequest, authResult.getUser(), authResult.isTemporaryPassword());
    }

    @GetMapping("/auth/check-nickname")
    @Override
    public ResponseEntity<ApiResponse<?>> checkNickname(@RequestParam("nickname") @NotBlank String nickname) {
        CheckResponse response = nicknameService.check(nickname);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PostMapping("/auth/refresh")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt == null || refreshJwt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure("AUTH401-INVALID_REFRESH", "리프레시 토큰 없음", null));
        }

        TokenIssuer.RefreshResult refreshResult = tokenIssuer.refresh(refreshJwt);
        if (!refreshResult.isSuccess()) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.onFailure(
                            "AUTH401-INVALID_REFRESH", refreshFailureMessage(refreshResult.failure()), null));
        }

        TokenIssuer.IssuedTokens issuedTokens = refreshResult.issuedTokens();

        ResponseCookie accessCookie =
                buildCookie(request, accessCookieName, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie =
                buildCookie(request, refreshCookieName, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(null)));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> buildTokenResponse(
            HttpServletRequest request, com.fmi.domain.user.data.User user, boolean isTemporaryPassword) {
        TokenIssuer.IssuedTokens issuedTokens = tokenIssuer.issue(user, isTemporaryPassword, null);

        ResponseCookie accessCookie =
                buildCookie(request, accessCookieName, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie =
                buildCookie(request, refreshCookieName, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(user.getId(), isTemporaryPassword)));
    }

    private ResponseCookie buildCookie(
            HttpServletRequest request, String name, String value, java.util.Date expiration) {
        return cookieFactory.build(
                request, name, value, java.time.Duration.between(java.time.Instant.now(), expiration.toInstant()));
    }

    @PostMapping("/auth/logout")
    @Override
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        String refreshJwt = getCookieValue(request, refreshCookieName);
        if (refreshJwt != null && !refreshJwt.isEmpty()) {
            tokenIssuer.revokeIfValid(refreshJwt);
        }

        // accessToken 쿠키 제거
        ResponseCookie removeAccess = cookieFactory.expire(request, accessCookieName);

        // refreshToken 쿠키 제거
        ResponseCookie removeRefresh = cookieFactory.expire(request, refreshCookieName);

        return ResponseEntity.ok()
                .header("Set-Cookie", removeAccess.toString())
                .header("Set-Cookie", removeRefresh.toString())
                .body(ApiResponse.onSuccess("OK"));
    }

    @PostMapping("/users/me/password/verify")
    public ApiResponse<Void> verifyPassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordVerifyRequest request) {
        passwordService.verify(userDetails.getUsername(), request);
        return ApiResponse.onSuccess(null);
    }

    @PatchMapping("/users/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.change(userDetails.getUsername(), request.getNewPassword(), request.getNewPasswordConfirm());
        return ApiResponse.onSuccess(null);
    }

    @DeleteMapping("/users/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AccountDeleteRequest request,
            HttpServletRequest httpRequest) {
        withdrawalService.delete(userDetails.getUsername(), request);
        ResponseCookie removeAccess = cookieFactory.expire(httpRequest, accessCookieName);
        ResponseCookie removeRefresh = cookieFactory.expire(httpRequest, refreshCookieName);
        return ResponseEntity.ok()
                .header("Set-Cookie", removeAccess.toString())
                .header("Set-Cookie", removeRefresh.toString())
                .body(ApiResponse.onSuccess(null));
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

    private static String refreshFailureMessage(TokenIssuer.RefreshFailure failure) {
        return switch (failure) {
            case INVALID_TOKEN -> "유효하지 않은 리프레시";
            case MISSING_JTI -> "유효하지 않은 리프레시(jti 없음)";
            case HASH_MISMATCH -> "유효하지 않은 리프레시(대조 실패)";
            case USER_NOT_FOUND -> "유효하지 않은 리프레시(사용자 없음)";
        };
    }
}

package com.fmi.domain.auth.web.controller;

import com.fmi.domain.auth.converter.AuthConverter;
import com.fmi.domain.auth.data.IssuedTokens;
import com.fmi.domain.auth.security.cookie.AuthCookieFactory;
import com.fmi.domain.auth.security.cookie.AuthCookieResolver;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.service.TokenService;
import com.fmi.domain.auth.web.dto.request.LoginRequest;
import com.fmi.domain.auth.web.dto.request.SignupRequest;
import com.fmi.domain.auth.web.dto.response.LoginResponse;
import com.fmi.domain.auth.web.swagger.AuthSwagger;
import com.fmi.domain.user.user.service.NicknameService;
import com.fmi.domain.user.user.web.dto.response.CheckResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthSwagger {

    private final AuthService authService;

    private final NicknameService nicknameService;

    private final TokenService tokenService;

    private final AuthCookieFactory authCookieFactory;

    private final AuthCookieResolver authCookieResolver;

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
        return buildTokenResponse(httpRequest, authResult.user(), authResult.isTemporaryPassword());
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
        String refreshJwt = authCookieResolver
                .findRefreshToken(request)
                .filter(token -> !token.isEmpty())
                .orElseThrow(() -> new GeneralException(ErrorStatus._REFRESH_TOKEN_NOT_FOUND));
        IssuedTokens issuedTokens = tokenService.refresh(refreshJwt);

        ResponseCookie accessCookie = authCookieFactory.createAccessCookie(
                request, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie = authCookieFactory.createRefreshCookie(
                request, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(null)));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> buildTokenResponse(
            HttpServletRequest request, com.fmi.domain.user.user.data.User user, boolean isTemporaryPassword) {
        IssuedTokens issuedTokens = tokenService.issue(user, isTemporaryPassword, null);

        ResponseCookie accessCookie = authCookieFactory.createAccessCookie(
                request, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie = authCookieFactory.createRefreshCookie(
                request, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(AuthConverter.toLoginResponse(user.getId(), isTemporaryPassword)));
    }

    @PostMapping("/auth/logout")
    @Override
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        authCookieResolver
                .findRefreshToken(request)
                .filter(token -> !token.isEmpty())
                .ifPresent(tokenService::revoke);

        ResponseCookie accessCookie = authCookieFactory.expireAccessCookie(request);
        ResponseCookie refreshCookie = authCookieFactory.expireRefreshCookie(request);

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess("OK"));
    }
}

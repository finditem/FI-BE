package com.fmi.domain.auth.web.controller;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.auth.data.IssuedTokens;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.service.TokenService;
import com.fmi.domain.auth.web.dto.AppleLoginRequest;
import com.fmi.domain.auth.web.response.LoginResponse;
import com.fmi.external.oauth.apple.AppleOAuthClient;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.AuthCookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/apple")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 이메일 인증, 소셜 로그인과 비밀번호 관리를 제공합니다.")
public class AppleAuthController {

    private final AppleOAuthClient appleOAuthService;
    private final SocialLoginService socialLoginService;
    private final TokenService tokenService;
    private final AuthCookieFactory authCookieFactory;

    @PostMapping
    @Operation(summary = "Apple 로그인", description = "Apple 인증 코드로 로그인하거나 회원가입합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithApple(
            @Valid @RequestBody AppleLoginRequest request, HttpServletRequest httpServletRequest) {
        String subject = appleOAuthService.exchangeCodeForSubject(request.getCode(), request.getEnvironment());
        var localUser = socialLoginService.upsertUserFromApple(subject).user();
        boolean termsAgreed = localUser.isPrivacyPolicyAgreed() && localUser.isTermsOfServiceAgreed();

        IssuedTokens issuedTokens = tokenService.issue(localUser, false, Provider.APPLE);

        ResponseCookie accessCookie = authCookieFactory.createAccessCookie(
                httpServletRequest, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie = authCookieFactory.createRefreshCookie(
                httpServletRequest, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(new LoginResponse(localUser.getId(), false, termsAgreed)));
    }
}

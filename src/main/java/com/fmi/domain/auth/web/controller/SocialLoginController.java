package com.fmi.domain.auth.web.controller;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.auth.data.IssuedTokens;
import com.fmi.domain.auth.data.SocialLoginCommand;
import com.fmi.domain.auth.security.cookie.AuthCookieFactory;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.service.TokenService;
import com.fmi.domain.auth.web.dto.AppleLoginRequest;
import com.fmi.domain.auth.web.dto.KakaoLoginRequest;
import com.fmi.domain.auth.web.response.LoginResponse;
import com.fmi.domain.auth.web.swagger.KakaoAuthSwagger;
import com.fmi.domain.user.data.User;
import com.fmi.external.oauth.apple.AppleOAuthClient;
import com.fmi.external.oauth.kakao.KakaoOAuthClient;
import com.fmi.external.oauth.kakao.KakaoOAuthClient.KakaoToken;
import com.fmi.external.oauth.kakao.KakaoOAuthClient.KakaoUser;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SocialLoginController implements KakaoAuthSwagger {

    private final KakaoOAuthClient kakaoOAuthService;
    private final AppleOAuthClient appleOAuthService;
    private final SocialLoginService socialLoginService;
    private final TokenService tokenService;
    private final AuthCookieFactory authCookieFactory;

    @PostMapping("/auth/kakao")
    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequest req, HttpServletRequest request) {
        // environment에 따라 환경 변수에서 자동 선택
        KakaoToken token = kakaoOAuthService.exchangeCodeForToken(req.getCode(), req.getEnvironment());
        String kakaoAccessToken = token.getAccess_token();

        KakaoUser user = kakaoOAuthService.getUserInfo(kakaoAccessToken);
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

        String providerId = String.valueOf(user.getId());
        SocialLoginCommand command = new SocialLoginCommand(Provider.KAKAO, providerId, nickname, profile);
        User localUser = socialLoginService.login(command);
        return buildLoginResponse(request, localUser, Provider.KAKAO);
    }

    @PostMapping("/auth/apple")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithApple(
            @Valid @RequestBody AppleLoginRequest request, HttpServletRequest httpServletRequest) {
        String subject = appleOAuthService.exchangeCodeForSubject(request.getCode(), request.getEnvironment());
        SocialLoginCommand command = new SocialLoginCommand(Provider.APPLE, subject, null, null);
        User localUser = socialLoginService.login(command);
        return buildLoginResponse(httpServletRequest, localUser, Provider.APPLE);
    }

    private ResponseEntity<ApiResponse<LoginResponse>> buildLoginResponse(
            HttpServletRequest request, User localUser, Provider provider) {
        boolean termsAgreed = localUser.isPrivacyPolicyAgreed() && localUser.isTermsOfServiceAgreed();
        IssuedTokens issuedTokens = tokenService.issue(localUser, false, provider);
        ResponseCookie accessCookie = authCookieFactory.createAccessCookie(
                request, issuedTokens.accessToken(), issuedTokens.accessExpiration());
        ResponseCookie refreshCookie = authCookieFactory.createRefreshCookie(
                request, issuedTokens.refreshToken(), issuedTokens.refreshExpiration());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(new LoginResponse(localUser.getId(), false, termsAgreed)));
    }
}

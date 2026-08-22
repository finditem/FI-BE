package com.fmi.domain.auth.web.controller;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.service.TokenIssuer;
import com.fmi.domain.auth.web.dto.KakaoLoginRequest;
import com.fmi.external.oauth.kakao.KakaoOAuthClient;
import com.fmi.external.oauth.kakao.KakaoOAuthClient.KakaoToken;
import com.fmi.external.oauth.kakao.KakaoOAuthClient.KakaoUser;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class KakaoAuthController {

    private final KakaoOAuthClient kakaoOAuthService;
    private final SocialLoginService socialLoginService;
    private final TokenIssuer tokenIssuer;
    private final CookieFactory cookieFactory;

    @Value("${jwt.cookie.name:refresh_token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.access-token-name:access_token}")
    private String accessCookieName;

    @PostMapping
    @Operation(summary = "카카오 로그인", description = """
                   카카오 인증 코드를 사용하여 로그인합니다.

                   **동작 방식:**
                   - 카카오 OAuth 인증 코드를 받아 토큰을 교환하고 사용자 정보를 조회합니다.
                   - 기존 사용자가 있으면 로그인, 없으면 자동 회원가입 후 로그인합니다.
                   - 소셜 로그인이므로 임시 비밀번호 기능은 사용하지 않습니다 (isTemporaryPassword는 항상 false).

                   **주의사항:**
                   - 카카오 인증 코드는 **한 번만 사용 가능**합니다. (재사용 시 AUTH400-KAKAO_CODE_INVALID 에러 발생)
                   - 성공 시 `access_token`과 `refresh_token`이 **쿠키**로 설정됩니다. (응답 body에 포함되지 않음)
                   - 인증 코드는 프론트엔드에서 카카오 로그인 플로우를 통해 받아야 합니다.
                   """)
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "카카오 로그인 성공 - access_token과 refresh_token은 Set-Cookie 헤더로 전송됩니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                name = "성공 응답 예시",
                                                value = """
                                    {
                                      "isSuccess": true,
                                      "code": "COMMON200",
                                      "message": "성공입니다.",
                                      "result": {
                                        "userId": 1,
                                        "isTemporaryPassword": false,
                                        "termsAgreed": false
                                      }
                                    }
                                    """,
                                                summary = "로그인 성공 시 응답 (termsAgreed: true=약관동의완료, false=약관동의필요)"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "AUTH400-KAKAO_CODE_INVALID: 카카오 인증 코드가 유효하지 않거나 이미 사용되었습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH400-KAKAO_CODE_INVALID\", \"message\": \"카카오 인증 코드가 유효하지 않거나 이미 사용되었습니다. 다시 로그인해주세요.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "AUTH500-KAKAO_TOKEN_FAILED: 카카오 토큰 교환에 실패했습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH500-KAKAO_TOKEN_FAILED\", \"message\": \"카카오 토큰 교환에 실패했습니다.\"}"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "AUTH500-KAKAO_USERINFO_FAILED: 카카오 사용자 정보 조회에 실패했습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"AUTH500-KAKAO_USERINFO_FAILED\", \"message\": \"카카오 사용자 정보 조회에 실패했습니다.\"}")))
    })
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequest req, HttpServletRequest request) {
        // environment에 따라 환경 변수에서 자동 선택
        KakaoToken token = kakaoOAuthService.exchangeCodeForToken(req.getCode(), req.getEnvironment());
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

        var result = socialLoginService.upsertUserFromKakao(user.getId(), email, nickname, profile);
        var localUser = result.user();
        boolean termsAgreed = localUser.isPrivacyPolicyAgreed() && localUser.isTermsOfServiceAgreed();

        TokenIssuer.IssuedTokens issuedTokens = tokenIssuer.issue(localUser, false, Provider.KAKAO);

        // accessToken 쿠키 설정
        ResponseCookie accessCookie = cookieFactory.build(
                request,
                accessCookieName,
                issuedTokens.accessToken(),
                java.time.Duration.between(
                        java.time.Instant.now(), issuedTokens.accessExpiration().toInstant()));

        // refreshToken 쿠키 설정
        ResponseCookie refreshCookie = cookieFactory.build(
                request,
                refreshCookieName,
                issuedTokens.refreshToken(),
                java.time.Duration.between(
                        java.time.Instant.now(),
                        issuedTokens.refreshExpiration().toInstant()));

        // 응답 생성 및 반환
        // accessToken과 refreshToken은 쿠키로 전송되므로 응답 body에는 포함하지 않습니다.
        // 소셜 로그인은 임시 비밀번호 기능이 없으므로 isTemporaryPassword는 항상 false입니다.
        return ResponseEntity.ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(ApiResponse.onSuccess(new LoginResponse(localUser.getId(), false, termsAgreed)));
    }
}

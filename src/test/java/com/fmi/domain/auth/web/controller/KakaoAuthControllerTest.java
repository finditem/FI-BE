package com.fmi.domain.auth.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.domain.auth.service.KakaoOAuthService;
import com.fmi.domain.auth.service.KakaoOAuthService.KakaoToken;
import com.fmi.domain.auth.service.KakaoOAuthService.KakaoUser;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.web.dto.KakaoLoginRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class KakaoAuthControllerTest {

    @Mock
    private KakaoOAuthService kakaoOAuthService;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private CookieFactory cookieFactory;

    @Captor
    private ArgumentCaptor<Map<String, Object>> claimsCaptor;

    @InjectMocks
    private KakaoAuthController kakaoAuthController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kakaoAuthController, "accessCookieName", "access_token");
        ReflectionTestUtils.setField(kakaoAuthController, "refreshCookieName", "refresh_token");
    }

    @Nested
    @DisplayName("카카오 로그인")
    class LoginWithKakao {

        @Nested
        @DisplayName("카카오 OAuth 연동이 성공하면")
        class WithSuccessfulKakaoOAuth {

            @Test
            @DisplayName("KAKAO 제공자 클레임과 두 쿠키를 반환한다")
            void returnsProviderClaimAndCookies() {
                // given
                String email = "member@finditem.kr";
                User localUser = User.builder()
                        .id(1L)
                        .email(email)
                        .role(Role.USER)
                        .privacyPolicyAgreed(true)
                        .termsOfServiceAgreed(true)
                        .build();
                KakaoLoginRequest request = new KakaoLoginRequest("authorization-code", "dev");
                MockHttpServletRequest httpRequest = new MockHttpServletRequest();
                Date accessExpiration = Date.from(Instant.now().plusSeconds(900));
                Date refreshExpiration = Date.from(Instant.now().plusSeconds(1_200));
                KakaoToken kakaoToken = new KakaoToken("bearer", "kakao-access-token", 300, null, null, null);
                KakaoUser kakaoUser = new KakaoUser(
                        100L,
                        new KakaoUser.KakaoAccount(
                                email, new KakaoUser.KakaoAccount.Profile("카카오토끼", "https://example.com/profile.png")),
                        null);

                when(kakaoOAuthService.exchangeCodeForToken("authorization-code", "dev"))
                        .thenReturn(kakaoToken);
                when(kakaoOAuthService.getUserInfo("kakao-access-token")).thenReturn(kakaoUser);
                when(socialLoginService.upsertUserFromKakao(100L, email, "카카오토끼", "https://example.com/profile.png"))
                        .thenReturn(new SocialLoginService.KakaoLoginResult(localUser));
                when(jwtTokenProvider.createAccessToken(eq(email), any())).thenReturn("access-token");
                when(jwtTokenProvider.createRefreshToken(eq(email), anyString()))
                        .thenReturn("refresh-token");
                when(jwtTokenProvider.getExpiration("access-token")).thenReturn(accessExpiration);
                when(jwtTokenProvider.getExpiration("refresh-token")).thenReturn(refreshExpiration);
                when(cookieFactory.build(eq(httpRequest), eq("access_token"), eq("access-token"), any()))
                        .thenReturn(ResponseCookie.from("access_token", "access-token")
                                .build());
                when(cookieFactory.build(eq(httpRequest), eq("refresh_token"), eq("refresh-token"), any()))
                        .thenReturn(ResponseCookie.from("refresh_token", "refresh-token")
                                .build());

                // when
                ResponseEntity<ApiResponse<LoginResponse>> response =
                        kakaoAuthController.loginWithKakao(request, httpRequest);

                // then
                verify(jwtTokenProvider).createAccessToken(eq(email), claimsCaptor.capture());
                verify(refreshTokenStore).issue(anyString(), eq(email), anyString(), eq(refreshExpiration.toInstant()));
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getHeaders().get("Set-Cookie"))
                        .containsExactly("access_token=access-token", "refresh_token=refresh-token");
                assertThat(response.getBody().getResult()).isEqualTo(new LoginResponse(1L, false, true));
                assertThat(claimsCaptor.getValue())
                        .containsEntry("userId", 1L)
                        .containsEntry("role", "USER")
                        .containsEntry("provider", "KAKAO")
                        .containsEntry("purpose", "access");
            }
        }
    }
}

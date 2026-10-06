package com.fmi.domain.auth.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.IssuedTokens;
import com.fmi.domain.auth.data.SocialLoginCommand;
import com.fmi.domain.auth.security.cookie.AuthCookieFactory;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.service.TokenService;
import com.fmi.domain.auth.web.dto.AppleLoginRequest;
import com.fmi.domain.auth.web.dto.KakaoLoginRequest;
import com.fmi.domain.auth.web.response.LoginResponse;
import com.fmi.domain.user.data.User;
import com.fmi.external.oauth.apple.AppleOAuthClient;
import com.fmi.external.oauth.kakao.KakaoOAuthClient;
import com.fmi.external.oauth.kakao.KakaoOAuthClient.KakaoToken;
import com.fmi.external.oauth.kakao.KakaoOAuthClient.KakaoUser;
import com.fmi.global.apiPayload.ApiResponse;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SocialLoginControllerTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthService;

    @Mock
    private AppleOAuthClient appleOAuthService;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthCookieFactory authCookieFactory;

    @InjectMocks
    private SocialLoginController socialLoginController;

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
                SocialLoginCommand command =
                        new SocialLoginCommand(Provider.KAKAO, "100", "카카오토끼", "https://example.com/profile.png");
                when(socialLoginService.login(command)).thenReturn(localUser);
                when(tokenService.issue(localUser, false, Provider.KAKAO))
                        .thenReturn(
                                new IssuedTokens("access-token", accessExpiration, "refresh-token", refreshExpiration));
                when(authCookieFactory.createAccessCookie(httpRequest, "access-token", accessExpiration))
                        .thenReturn(ResponseCookie.from("access_token", "access-token")
                                .build());
                when(authCookieFactory.createRefreshCookie(httpRequest, "refresh-token", refreshExpiration))
                        .thenReturn(ResponseCookie.from("refresh_token", "refresh-token")
                                .build());

                // when
                ResponseEntity<ApiResponse<LoginResponse>> response =
                        socialLoginController.loginWithKakao(request, httpRequest);

                // then
                verify(tokenService).issue(localUser, false, Provider.KAKAO);
                verify(socialLoginService).login(command);
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getHeaders().get("Set-Cookie"))
                        .containsExactly("access_token=access-token", "refresh_token=refresh-token");
                assertThat(response.getBody().getResult()).isEqualTo(new LoginResponse(1L, false, true));
            }
        }
    }

    @Nested
    @DisplayName("Apple 로그인")
    class LoginWithApple {

        @Test
        @DisplayName("Apple OAuth 연동이 성공하면 APPLE 제공자 클레임과 두 쿠키를 반환한다")
        void returnsProviderClaimAndCookies() {
            // given
            AppleLoginRequest request = new AppleLoginRequest("apple-code", "dev");
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();
            User user = User.builder()
                    .id(1L)
                    .email("apple_apple-subject@apple.local")
                    .role(Role.USER)
                    .build();
            SocialLoginCommand command = new SocialLoginCommand(Provider.APPLE, "apple-subject", null, null);
            when(appleOAuthService.exchangeCodeForSubject("apple-code", "dev")).thenReturn("apple-subject");
            when(socialLoginService.login(command)).thenReturn(user);
            Date accessExpiration = Date.from(Instant.now().plusSeconds(900));
            Date refreshExpiration = Date.from(Instant.now().plusSeconds(1_200));
            when(tokenService.issue(user, false, Provider.APPLE))
                    .thenReturn(new IssuedTokens("access-token", accessExpiration, "refresh-token", refreshExpiration));
            when(authCookieFactory.createAccessCookie(httpRequest, "access-token", accessExpiration))
                    .thenReturn(
                            ResponseCookie.from("access_token", "access-token").build());
            when(authCookieFactory.createRefreshCookie(httpRequest, "refresh-token", refreshExpiration))
                    .thenReturn(ResponseCookie.from("refresh_token", "refresh-token")
                            .build());

            // when
            ResponseEntity<ApiResponse<LoginResponse>> response =
                    socialLoginController.loginWithApple(request, httpRequest);

            // then
            verify(socialLoginService).login(command);
            verify(tokenService).issue(user, false, Provider.APPLE);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getHeaders().get("Set-Cookie"))
                    .containsExactly("access_token=access-token", "refresh_token=refresh-token");
            assertThat(response.getBody().getResult()).isEqualTo(new LoginResponse(1L, false, false));
        }
    }

    @Test
    @DisplayName("기존 카카오와 Apple 로그인 경로를 유지한다")
    void keepsSocialLoginRoutes() throws Exception {
        var mockMvc = MockMvcBuilders.standaloneSetup(socialLoginController).build();

        mockMvc.perform(post("/auth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/auth/apple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest());
    }
}

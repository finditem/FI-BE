package com.fmi.domain.auth.web.controller;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.service.AppleOAuthService;
import com.fmi.domain.auth.service.SocialLoginService;
import com.fmi.domain.auth.web.dto.AppleLoginRequest;
import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleAuthControllerTest {

    @Mock
    private AppleOAuthService appleOAuthService;

    @Mock
    private SocialLoginService socialLoginService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private CookieFactory cookieFactory;

    @InjectMocks
    private AppleAuthController appleAuthController;

    @Test
    void Apple_로그인에_성공하면_두_JWT_쿠키와_로그인_응답을_반환한다() {
        // given
        ReflectionTestUtils.setField(appleAuthController, "accessCookieName", "access_token");
        ReflectionTestUtils.setField(appleAuthController, "refreshCookieName", "refresh_token");
        AppleLoginRequest request = new AppleLoginRequest("apple-code", "dev");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        User user = User.builder().id(1L).email("apple_apple-subject@apple.local").role(Role.USER).build();
        when(appleOAuthService.exchangeCodeForSubject("apple-code", "dev")).thenReturn("apple-subject");
        when(socialLoginService.upsertUserFromApple("apple-subject"))
                .thenReturn(new SocialLoginService.AppleLoginResult(user));
        when(jwtTokenProvider.createAccessToken(eq(user.getEmail()), any())).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(eq(user.getEmail()), anyString())).thenReturn("refresh-token");
        when(jwtTokenProvider.getExpiration("access-token")).thenReturn(Date.from(Instant.now().plusSeconds(900)));
        when(jwtTokenProvider.getExpiration("refresh-token")).thenReturn(Date.from(Instant.now().plusSeconds(1_200)));
        when(cookieFactory.build(eq(httpRequest), eq("access_token"), eq("access-token"), any()))
                .thenReturn(ResponseCookie.from("access_token", "access-token").build());
        when(cookieFactory.build(eq(httpRequest), eq("refresh_token"), eq("refresh-token"), any()))
                .thenReturn(ResponseCookie.from("refresh_token", "refresh-token").build());

        // when
        ResponseEntity<ApiResponse<LoginResponse>> response = appleAuthController.loginWithApple(request, httpRequest);

        // then
        ArgumentCaptor<String> refreshJtiCaptor = ArgumentCaptor.forClass(String.class);
        verify(refreshTokenStore).issue(
                refreshJtiCaptor.capture(),
                eq(user.getEmail()),
                anyString(),
                any()
        );
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode().value()).isEqualTo(200);
            softly.assertThat(response.getHeaders().get("Set-Cookie"))
                    .containsExactly("access_token=access-token", "refresh_token=refresh-token");
            softly.assertThat(response.getBody().getResult().getUserId()).isEqualTo(1L);
            softly.assertThat(response.getBody().getResult().isTemporaryPassword()).isFalse();
            softly.assertThat(response.getBody().getResult().isTermsAgreed()).isFalse();
            softly.assertThat(refreshJtiCaptor.getValue()).isNotBlank();
        });
    }
}

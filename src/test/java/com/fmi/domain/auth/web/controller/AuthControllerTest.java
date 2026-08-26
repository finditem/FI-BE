package com.fmi.domain.auth.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.Provider;
import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.SocialAccounts;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.response.LoginResponse;
import com.fmi.domain.auth.service.AuthService;
import com.fmi.domain.auth.web.dto.LoginRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
import com.fmi.security.JwtTokenProvider;
import com.fmi.security.RefreshTokenStore;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private SocialAccountsRepository socialAccountsRepository;

    @Mock
    private CookieFactory cookieFactory;

    @Captor
    private ArgumentCaptor<Map<String, Object>> claimsCaptor;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authController, "accessCookieName", "access_token");
        ReflectionTestUtils.setField(authController, "refreshCookieName", "refresh_token");
    }

    @Nested
    @DisplayName("토큰 갱신")
    class Refresh {

        @Nested
        @DisplayName("유효한 갱신 토큰 쿠키가 있으면")
        class WithValidRefreshCookie {

            @Test
            @DisplayName("기존 JTI를 폐기한 뒤 제공자 클레임과 두 쿠키를 발급한다")
            void 유효한_refresh는_기존_JTI를_폐기한_뒤_provider_claim과_두_쿠키를_발급한다() {
                // given
                String email = "member@finditem.kr";
                String oldRefreshToken = "old-refresh-token";
                String oldJti = "old-jti";
                String newAccessToken = "new-access-token";
                String newRefreshToken = "new-refresh-token";
                User user = User.builder().id(1L).email(email).role(Role.USER).build();
                SocialAccounts socialAccount =
                        SocialAccounts.builder().provider(Provider.KAKAO).build();
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setCookies(new Cookie("refresh_token", oldRefreshToken));
                Date accessExpiration = Date.from(Instant.now().plusSeconds(900));
                Date refreshExpiration = Date.from(Instant.now().plusSeconds(1_200));

                when(jwtTokenProvider.validateToken(oldRefreshToken)).thenReturn(true);
                when(jwtTokenProvider.getSubject(oldRefreshToken)).thenReturn(email);
                when(jwtTokenProvider.getJti(oldRefreshToken)).thenReturn(oldJti);
                when(refreshTokenStore.validate(eq(oldJti), anyString(), eq(email)))
                        .thenReturn(true);
                when(authService.findActiveUserByEmail(email)).thenReturn(Optional.of(user));
                when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.of(socialAccount));
                when(jwtTokenProvider.createAccessToken(eq(email), any())).thenReturn(newAccessToken);
                when(jwtTokenProvider.createRefreshToken(eq(email), anyString()))
                        .thenReturn(newRefreshToken);
                when(jwtTokenProvider.getExpiration(newAccessToken)).thenReturn(accessExpiration);
                when(jwtTokenProvider.getExpiration(newRefreshToken)).thenReturn(refreshExpiration);
                when(cookieFactory.build(eq(request), eq("access_token"), eq(newAccessToken), any()))
                        .thenReturn(ResponseCookie.from("access_token", newAccessToken)
                                .build());
                when(cookieFactory.build(eq(request), eq("refresh_token"), eq(newRefreshToken), any()))
                        .thenReturn(ResponseCookie.from("refresh_token", newRefreshToken)
                                .build());

                // when
                ResponseEntity<ApiResponse<LoginResponse>> response = authController.refresh(request);

                // then
                InOrder refreshStoreOrder = inOrder(refreshTokenStore);
                refreshStoreOrder.verify(refreshTokenStore).validate(eq(oldJti), anyString(), eq(email));
                refreshStoreOrder.verify(refreshTokenStore).revoke(oldJti);
                refreshStoreOrder
                        .verify(refreshTokenStore)
                        .issue(anyString(), eq(email), anyString(), eq(refreshExpiration.toInstant()));

                org.mockito.Mockito.verify(jwtTokenProvider).createAccessToken(eq(email), claimsCaptor.capture());
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getHeaders().get("Set-Cookie"))
                        .containsExactly("access_token=" + newAccessToken, "refresh_token=" + newRefreshToken);
                assertThat(response.getBody().getResult()).isEqualTo(new LoginResponse(null, false, true));
                assertThat(claimsCaptor.getValue())
                        .containsEntry("userId", 1L)
                        .containsEntry("role", "USER")
                        .containsEntry("purpose", "access")
                        .containsEntry("provider", "KAKAO");
            }
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Nested
        @DisplayName("갱신 토큰 쿠키가 없으면")
        class WithoutRefreshCookie {

            @Test
            @DisplayName("성공 응답과 두 만료 쿠키를 반환한다")
            void refresh_쿠키가_없어도_logout은_성공하고_두_쿠키를_만료한다() {
                // given
                MockHttpServletRequest request = new MockHttpServletRequest();
                when(cookieFactory.expire(request, "access_token"))
                        .thenReturn(ResponseCookie.from("access_token", "")
                                .maxAge(0)
                                .build());
                when(cookieFactory.expire(request, "refresh_token"))
                        .thenReturn(ResponseCookie.from("refresh_token", "")
                                .maxAge(0)
                                .build());

                // when
                ResponseEntity<ApiResponse<String>> response = authController.logout(request);

                // then
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getHeaders().get("Set-Cookie"))
                        .containsExactly(
                                "access_token=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT",
                                "refresh_token=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
                assertThat(response.getBody().getResult()).isEqualTo("OK");
                verifyNoInteractions(jwtTokenProvider, refreshTokenStore);
            }
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Nested
        @DisplayName("임시 비밀번호 인증 결과면")
        class WithTemporaryPassword {

            @Test
            @DisplayName("임시 클레임과 두 쿠키를 발급한다")
            void 임시_비밀번호_로그인은_임시_claim과_두_쿠키를_발급한다() {
                // given
                String email = "member@finditem.kr";
                String accessToken = "access-token";
                String refreshToken = "refresh-token";
                User user = User.builder().id(1L).email(email).role(Role.USER).build();
                LoginRequest request = new LoginRequest();
                request.setEmail(email);
                request.setPassword("temporary-password");
                MockHttpServletRequest httpRequest = new MockHttpServletRequest();
                Date accessExpiration = Date.from(Instant.now().plusSeconds(900));
                Date refreshExpiration = Date.from(Instant.now().plusSeconds(1_200));

                when(authService.authenticate(email, "temporary-password"))
                        .thenReturn(new AuthService.AuthenticateResult(user, true));
                when(jwtTokenProvider.createAccessToken(eq(email), any())).thenReturn(accessToken);
                when(jwtTokenProvider.createRefreshToken(eq(email), anyString()))
                        .thenReturn(refreshToken);
                when(jwtTokenProvider.getExpiration(accessToken)).thenReturn(accessExpiration);
                when(jwtTokenProvider.getExpiration(refreshToken)).thenReturn(refreshExpiration);
                when(cookieFactory.build(eq(httpRequest), eq("access_token"), eq(accessToken), any()))
                        .thenReturn(
                                ResponseCookie.from("access_token", accessToken).build());
                when(cookieFactory.build(eq(httpRequest), eq("refresh_token"), eq(refreshToken), any()))
                        .thenReturn(ResponseCookie.from("refresh_token", refreshToken)
                                .build());

                // when
                ResponseEntity<ApiResponse<LoginResponse>> response = authController.login(request, httpRequest);

                // then
                org.mockito.Mockito.verify(jwtTokenProvider).createAccessToken(eq(email), claimsCaptor.capture());
                assertThat(response.getStatusCode().value()).isEqualTo(200);
                assertThat(response.getHeaders().get("Set-Cookie"))
                        .containsExactly("access_token=" + accessToken, "refresh_token=" + refreshToken);
                assertThat(response.getBody().getResult()).isEqualTo(new LoginResponse(1L, true, true));
                assertThat(claimsCaptor.getValue())
                        .containsEntry("userId", 1L)
                        .containsEntry("role", "USER")
                        .containsEntry("purpose", "access")
                        .containsEntry("isTemporaryPassword", true)
                        .doesNotContainKey("provider");
            }
        }
    }
}

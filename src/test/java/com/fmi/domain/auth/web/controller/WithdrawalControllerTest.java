package com.fmi.domain.auth.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.data.enums.WithdrawalReason;
import com.fmi.domain.auth.security.cookie.AuthCookieFactory;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.web.dto.request.AccountDeleteRequest;
import com.fmi.global.apiPayload.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;

@ExtendWith(MockitoExtension.class)
@DisplayName("WithdrawalController")
class WithdrawalControllerTest {
    @Mock
    private WithdrawalService withdrawalService;

    @Mock
    private AuthCookieFactory authCookieFactory;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private WithdrawalController withdrawalController;

    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteAccount {

        @Test
        @DisplayName("기존 회원 탈퇴 경로를 유지한다")
        void keepsAccountDeletePath() throws NoSuchMethodException {
            // given
            Class<WithdrawalController> controllerType = WithdrawalController.class;

            // when
            DeleteMapping mapping = controllerType
                    .getMethod(
                            "deleteAccount",
                            UserDetails.class,
                            AccountDeleteRequest.class,
                            jakarta.servlet.http.HttpServletRequest.class)
                    .getAnnotation(DeleteMapping.class);

            // then
            assertThat(mapping.value()).containsExactly("/users/me");
        }

        @Nested
        @DisplayName("탈퇴 요청이 유효하면")
        class WithValidRequest {

            @Test
            @DisplayName("탈퇴를 처리하고 액세스 쿠키와 리프레시 쿠키를 순서대로 만료한다")
            void deletesAccountAndExpiresCookiesInOrder() {
                // given
                String email = "member@finditem.kr";
                AccountDeleteRequest request = new AccountDeleteRequest();
                request.setReasons(List.of(WithdrawalReason.NOT_USING));
                MockHttpServletRequest httpRequest = new MockHttpServletRequest();
                when(userDetails.getUsername()).thenReturn(email);
                when(authCookieFactory.expireAccessCookie(httpRequest)).thenReturn(accessCookie(""));
                when(authCookieFactory.expireRefreshCookie(httpRequest)).thenReturn(refreshCookie(""));

                // when
                ResponseEntity<ApiResponse<Void>> response =
                        withdrawalController.deleteAccount(userDetails, request, httpRequest);

                // then
                verify(withdrawalService).delete(email, request);
                assertThat(response.getHeaders().get("Set-Cookie")).containsExactly("access_token=", "refresh_token=");
            }
        }
    }

    private static ResponseCookie accessCookie(String token) {
        return ResponseCookie.from("access_token", token).build();
    }

    private static ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from("refresh_token", token).build();
    }
}

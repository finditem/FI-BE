package com.fmi.domain.auth.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.service.WithdrawalService;
import com.fmi.domain.auth.web.dto.AccountDeleteRequest;
import com.fmi.domain.auth.web.dto.PasswordChangeRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.security.CookieFactory;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountAuthControllerTest {

    @Mock
    private PasswordService passwordService;

    @Mock
    private WithdrawalService withdrawalService;

    @Mock
    private CookieFactory cookieFactory;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AccountAuthController accountAuthController;

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("요청 값을 해체해 비밀번호 유스케이스에 전달한다")
        void passesRequestValuesToPasswordService() {
            // given
            String email = "member@finditem.kr";
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setNewPassword("NewPassword1!");
            request.setNewPasswordConfirm("NewPassword1!");
            when(userDetails.getUsername()).thenReturn(email);

            // when
            accountAuthController.changePassword(userDetails, request);

            // then
            verify(passwordService).change(email, "NewPassword1!", "NewPassword1!");
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteAccount {

        @Nested
        @DisplayName("탈퇴 요청이 유효하면")
        class WithValidRequest {

            @Test
            @DisplayName("탈퇴를 처리하고 액세스 쿠키와 리프레시 쿠키를 순서대로 만료한다")
            void deletesAccountAndExpiresCookiesInOrder() {
                String email = "member@finditem.kr";
                AccountDeleteRequest request = new AccountDeleteRequest();
                request.setReasons(List.of(WithdrawalReason.NOT_USING));
                MockHttpServletRequest httpRequest = new MockHttpServletRequest();
                ReflectionTestUtils.setField(accountAuthController, "accessCookieName", "access_token");
                ReflectionTestUtils.setField(accountAuthController, "refreshCookieName", "refresh_token");
                when(userDetails.getUsername()).thenReturn(email);
                when(cookieFactory.expire(eq(httpRequest), eq("access_token")))
                        .thenReturn(ResponseCookie.from("access_token", "").build());
                when(cookieFactory.expire(eq(httpRequest), eq("refresh_token")))
                        .thenReturn(ResponseCookie.from("refresh_token", "").build());

                ResponseEntity<ApiResponse<Void>> response =
                        accountAuthController.deleteAccount(userDetails, request, httpRequest);

                verify(withdrawalService).delete(email, request);
                assertThat(response.getHeaders().get("Set-Cookie")).containsExactly("access_token=", "refresh_token=");
            }
        }
    }
}

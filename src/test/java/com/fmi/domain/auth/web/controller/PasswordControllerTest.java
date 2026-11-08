package com.fmi.domain.auth.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.service.PasswordService;
import com.fmi.domain.auth.web.dto.request.PasswordChangeRequest;
import com.fmi.domain.auth.web.dto.request.PasswordVerifyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordController")
class PasswordControllerTest {
    @Mock
    private PasswordService passwordService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private PasswordController passwordController;

    @Nested
    @DisplayName("비밀번호 검증")
    class VerifyPassword {

        @Test
        @DisplayName("기존 비밀번호 검증 경로를 유지한다")
        void keepsPasswordVerifyPath() throws NoSuchMethodException {
            // given
            Class<PasswordController> controllerType = PasswordController.class;

            // when
            PostMapping mapping = controllerType
                    .getMethod("verifyPassword", UserDetails.class, PasswordVerifyRequest.class)
                    .getAnnotation(PostMapping.class);

            // then
            assertThat(mapping.value()).containsExactly("/users/me/password/verify");
        }

        @Test
        @DisplayName("현재 비밀번호 검증을 비밀번호 유스케이스에 전달한다")
        void verifiesCurrentPassword() {
            // given
            String email = "member@finditem.kr";
            PasswordVerifyRequest request = new PasswordVerifyRequest();
            request.setCurrentPassword("CurrentPassword1!");
            when(userDetails.getUsername()).thenReturn(email);

            // when
            passwordController.verifyPassword(userDetails, request);

            // then
            verify(passwordService).verify(email, request);
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("기존 비밀번호 변경 경로를 유지한다")
        void keepsPasswordChangePath() throws NoSuchMethodException {
            // given
            Class<PasswordController> controllerType = PasswordController.class;

            // when
            PatchMapping mapping = controllerType
                    .getMethod("changePassword", UserDetails.class, PasswordChangeRequest.class)
                    .getAnnotation(PatchMapping.class);

            // then
            assertThat(mapping.value()).containsExactly("/users/me/password");
        }

        @Test
        @DisplayName("요청 값을 해체해 비밀번호 유스케이스에 전달한다")
        void changesPassword() {
            // given
            String email = "member@finditem.kr";
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setNewPassword("NewPassword1!");
            request.setNewPasswordConfirm("NewPassword1!");
            when(userDetails.getUsername()).thenReturn(email);

            // when
            passwordController.changePassword(userDetails, request);

            // then
            verify(passwordService).change(email, "NewPassword1!", "NewPassword1!");
        }
    }
}

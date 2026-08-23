package com.fmi.domain.auth.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordValidator")
class PasswordValidatorTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-23T03:00:00Z"), ZoneOffset.UTC);
    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator(passwordEncoder, clock);
    }

    @Test
    @DisplayName("새 비밀번호와 확인이 다르면 기존 불일치 예외를 던진다")
    void throwsExistingMismatchException() {
        // when & then
        assertThatThrownBy(() -> passwordValidator.validateConfirmation("NewPassword1!", "DifferentPassword1!"))
                .isInstanceOfSatisfying(GeneralException.class, exception -> org.assertj.core.api.Assertions.assertThat(
                                exception.getCode())
                        .isEqualTo(ErrorStatus._PASSWORD_CONFIRMATION_MISMATCH));
    }

    @Test
    @DisplayName("활성 임시 비밀번호가 일치하면 임시 인증 결과를 반환한다")
    void returnsTemporaryResultForMatchingActiveTemporaryPassword() {
        // given
        User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 4, 0));
        when(passwordEncoder.matches("temporary-password", "temporary-password-hash"))
                .thenReturn(true);

        // when
        PasswordValidator.CurrentPasswordValidationResult result =
                passwordValidator.validateCurrentPassword(user, "temporary-password");

        // then
        assertThat(result).isEqualTo(PasswordValidator.CurrentPasswordValidationResult.TEMPORARY);
    }

    @Test
    @DisplayName("만료된 임시 비밀번호 상태에서는 원래 비밀번호만 일반 인증 결과로 반환한다")
    void returnsStandardResultForOriginalPasswordAfterExpiry() {
        // given
        User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 3, 0));
        when(passwordEncoder.matches("original-password", "original-password-hash"))
                .thenReturn(true);

        // when
        PasswordValidator.CurrentPasswordValidationResult result =
                passwordValidator.validateCurrentPassword(user, "original-password");

        // then
        assertThat(result).isEqualTo(PasswordValidator.CurrentPasswordValidationResult.STANDARD);
    }

    @Test
    @DisplayName("만료된 임시 비밀번호는 비교하지 않고 실패 결과를 반환한다")
    void rejectsExpiredTemporaryPasswordWithoutMatchingIt() {
        // given
        User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 3, 0));

        // when
        PasswordValidator.CurrentPasswordValidationResult result =
                passwordValidator.validateCurrentPassword(user, "temporary-password");

        // then
        assertThat(result).isEqualTo(PasswordValidator.CurrentPasswordValidationResult.FAILED);
        verify(passwordEncoder, never()).matches("temporary-password", "temporary-password-hash");
    }

    private User temporaryPasswordUser(LocalDateTime expiresAt) {
        return User.builder()
                .password("temporary-password-hash")
                .originalPassword("original-password-hash")
                .temporaryPassword("temporary-password-hash")
                .temporaryPasswordExpiresAt(expiresAt)
                .build();
    }
}

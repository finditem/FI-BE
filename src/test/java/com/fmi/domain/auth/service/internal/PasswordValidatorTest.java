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
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("새 비밀번호를 검증할 때")
    class DescribeValidateNewPassword {

        @Nested
        @DisplayName("정책에 맞지 않는 비밀번호면")
        class ContextWithWeakPassword {

            @Test
            @DisplayName("약한 비밀번호 예외를 던진다")
            void itThrowsWeakPassword() {
                assertThatThrownBy(() -> passwordValidator.validateNewPassword("short"))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._WEAK_PASSWORD));
            }
        }
    }

    @Nested
    @DisplayName("새 비밀번호 확인값을 검증할 때")
    class DescribeValidateConfirmation {

        @Nested
        @DisplayName("새 비밀번호와 확인값이 다르면")
        class ContextWithMismatchedConfirmation {

            @Test
            @DisplayName("비밀번호 확인 불일치 예외를 던진다")
            void itThrowsPasswordConfirmationMismatch() {
                assertThatThrownBy(() -> passwordValidator.validateConfirmation("NewPassword1!", "DifferentPassword1!"))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._PASSWORD_CONFIRMATION_MISMATCH));
            }
        }
    }

    @Nested
    @DisplayName("활성 임시 비밀번호인지 확인할 때")
    class DescribeMatchesActiveTemporaryPassword {

        @Nested
        @DisplayName("활성 임시 비밀번호가 일치하면")
        class ContextWithMatchingActiveTemporaryPassword {

            @Test
            @DisplayName("임시 비밀번호 사용 여부를 반환한다")
            void itReturnsTemporaryPasswordUsage() {
                // given
                User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 4, 0));
                when(passwordEncoder.matches("temporary-password", "temporary-password-hash"))
                        .thenReturn(true);

                // when
                boolean isTemporaryPassword =
                        passwordValidator.matchesActiveTemporaryPassword(user, "temporary-password");

                // then
                assertThat(isTemporaryPassword).isTrue();
            }
        }

        @Nested
        @DisplayName("임시 비밀번호가 만료되고 원래 비밀번호가 일치하면")
        class ContextWithExpiredTemporaryPasswordAndMatchingOriginalPassword {

            @Test
            @DisplayName("임시 비밀번호를 사용하지 않았음을 반환한다")
            void itReturnsStandardPasswordUsage() {
                // given
                User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 3, 0));
                when(passwordEncoder.matches("original-password", "original-password-hash"))
                        .thenReturn(true);

                // when
                boolean isTemporaryPassword =
                        passwordValidator.matchesActiveTemporaryPassword(user, "original-password");
                passwordValidator.validateLoginPassword(user, "original-password");

                // then
                assertThat(isTemporaryPassword).isFalse();
            }
        }

        @Nested
        @DisplayName("임시 비밀번호가 만료된 상태에서 임시 비밀번호를 입력하면")
        class ContextWithExpiredTemporaryPasswordAndTemporaryPassword {

            @Test
            @DisplayName("임시 비밀번호 비교 없이 로그인 실패 예외를 던진다")
            void itThrowsInvalidCredentialsWithoutMatchingTemporaryPassword() {
                // given
                User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 3, 0));

                // when
                assertThat(passwordValidator.matchesActiveTemporaryPassword(user, "temporary-password"))
                        .isFalse();
                assertThatThrownBy(() -> passwordValidator.validateLoginPassword(user, "temporary-password"))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._INVALID_CREDENTIALS));
                verify(passwordEncoder, never()).matches("temporary-password", "temporary-password-hash");
            }
        }
    }

    @Nested
    @DisplayName("계정 비밀번호를 확인할 때")
    class DescribeVerifyAccountPassword {

        @Test
        @DisplayName("활성 임시 비밀번호를 허용한다")
        void acceptsActiveTemporaryPassword() {
            // given
            User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 4, 0));
            when(passwordEncoder.matches("temporary-password", "temporary-password-hash"))
                    .thenReturn(true);

            // when & then
            assertThat(passwordValidator.matchesActiveTemporaryPassword(user, "temporary-password"))
                    .isTrue();
        }

        @Test
        @DisplayName("임시 비밀번호가 활성 상태여도 원래 비밀번호를 허용한다")
        void acceptsOriginalPasswordWhileTemporaryPasswordIsActive() {
            // given
            User user = temporaryPasswordUser(LocalDateTime.of(2026, 8, 23, 4, 0));
            when(passwordEncoder.matches("original-password", "temporary-password-hash"))
                    .thenReturn(false);
            when(passwordEncoder.matches("original-password", "original-password-hash"))
                    .thenReturn(true);

            // when & then
            assertThat(passwordValidator.matchesActiveTemporaryPassword(user, "original-password"))
                    .isFalse();
            passwordValidator.validateAccountPassword(user, "original-password");
        }
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

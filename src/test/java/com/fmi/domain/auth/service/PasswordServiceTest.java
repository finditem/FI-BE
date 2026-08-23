package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.auth.service.internal.PasswordGenerator;
import com.fmi.domain.auth.service.internal.PasswordValidator;
import com.fmi.domain.auth.web.dto.PasswordVerifyRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.security.RefreshTokenStore;
import com.fmi.service.EmailService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private SocialAccountsRepository socialAccountsRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordGenerator passwordGenerator;

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-23T03:00:00Z"), ZoneOffset.UTC);
    private PasswordValidator passwordValidator;
    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator(passwordEncoder, clock);
        passwordService = new PasswordService(
                userRepository,
                socialAccountsRepository,
                passwordEncoder,
                passwordValidator,
                refreshTokenStore,
                emailService,
                passwordGenerator,
                clock);
    }

    @Nested
    @DisplayName("임시 비밀번호 발급")
    class IssueTemporaryPassword {

        @Test
        @DisplayName("하나의 암호화 값을 로그인과 임시 상태에 함께 저장한 뒤 메일을 발송한다")
        void savesOneEncodedPasswordThenSendsMail() {
            // given
            String email = "member@finditem.kr";
            User user = User.builder()
                    .email(email)
                    .password("original-password-hash")
                    .build();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.empty());
            when(passwordGenerator.generateTemporaryPassword()).thenReturn("temporary-password");
            when(passwordEncoder.encode("temporary-password")).thenReturn("temporary-password-hash");

            // when
            passwordService.issueTemporaryPassword(email);

            // then
            InOrder order = inOrder(userRepository, emailService);
            order.verify(userRepository).save(user);
            order.verify(emailService)
                    .sendHtmlEmail(eq(email), eq("임시 비밀번호 발급"), eq("password-reset-email.html"), anyMap());
            verify(passwordGenerator).generateTemporaryPassword();
            assertThat(user.getOriginalPassword()).isEqualTo("original-password-hash");
            assertThat(user.getTemporaryPassword()).isEqualTo("temporary-password-hash");
            assertThat(user.getPassword()).isEqualTo("temporary-password-hash");
            assertThat(user.getTemporaryPasswordExpiresAt())
                    .isAfter(LocalDateTime.now().plusMinutes(59));
            assertThat(user.getTemporaryPasswordExpiresAt())
                    .isBefore(LocalDateTime.now().plusMinutes(61));
        }

        @Test
        @DisplayName("소셜 계정은 저장과 메일 발송 없이 거절한다")
        void rejectsSocialAccountWithoutSavingOrSendingMail() {
            // given
            String email = "member@finditem.kr";
            User user = User.builder().email(email).build();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.of(mockSocialAccount()));

            // when & then
            assertThatThrownBy(() -> passwordService.issueTemporaryPassword(email))
                    .isInstanceOf(RuntimeException.class);
            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
        }

        @Test
        @DisplayName("메일 발송 실패는 저장 이후 호출자에게 전파한다")
        void propagatesMailFailureAfterSaving() {
            // given
            String email = "member@finditem.kr";
            User user = User.builder()
                    .email(email)
                    .password("original-password-hash")
                    .build();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.empty());
            when(passwordGenerator.generateTemporaryPassword()).thenReturn("temporary-password");
            when(passwordEncoder.encode("temporary-password")).thenReturn("temporary-password-hash");
            doThrow(new IllegalStateException("mail unavailable"))
                    .when(emailService)
                    .sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());

            // when & then
            assertThatThrownBy(() -> passwordService.issueTemporaryPassword(email))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("mail unavailable");
            InOrder order = inOrder(userRepository, emailService);
            order.verify(userRepository).save(user);
            order.verify(emailService).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Nested
        @DisplayName("임시 비밀번호 상태의 사용자이면")
        class WithTemporaryPassword {

            @Test
            @DisplayName("임시 상태를 지우고 모든 토큰을 폐기한다")
            void clearsTemporaryStateAndRevokesAllTokens() {
                String email = "member@finditem.kr";
                User user = User.builder()
                        .email(email)
                        .password("temporary-password-hash")
                        .originalPassword("original-password-hash")
                        .temporaryPassword("temporary-password-hash")
                        .temporaryPasswordExpiresAt(LocalDateTime.now().plusHours(1))
                        .build();
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-password-hash");

                // when
                passwordService.change(email, "NewPassword1!", "NewPassword1!");

                InOrder order = inOrder(userRepository, refreshTokenStore);
                order.verify(userRepository).save(user);
                order.verify(refreshTokenStore).revokeAllForUser(email);
                assertThat(user.getPassword()).isEqualTo("new-password-hash");
                assertThat(user.getOriginalPassword()).isNull();
                assertThat(user.getTemporaryPassword()).isNull();
                assertThat(user.getTemporaryPasswordExpiresAt()).isNull();
                assertThat(user.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 23, 3, 0));
            }
        }

        @Test
        @DisplayName("새 비밀번호와 확인이 다르면 인코딩·저장·토큰 폐기 없이 기존 예외를 던진다")
        void rejectsMismatchedConfirmationBeforeEncoding() {
            // given
            String email = "member@finditem.kr";
            User user = User.builder().email(email).build();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> passwordService.change(email, "NewPassword1!", "DifferentPassword1!"))
                    .isInstanceOfSatisfying(
                            com.fmi.global.apiPayload.exception.GeneralException.class,
                            exception -> assertThat(exception.getCode())
                                    .isEqualTo(com.fmi
                                            .global
                                            .apiPayload
                                            .code
                                            .status
                                            .ErrorStatus
                                            ._PASSWORD_CONFIRMATION_MISMATCH));
            verify(userRepository, never()).save(user);
            verifyNoInteractions(passwordEncoder, refreshTokenStore);
        }
    }

    @Nested
    @DisplayName("현재 비밀번호 검증")
    class VerifyPassword {

        @Test
        @DisplayName("임시 비밀번호가 만료되면 원래 비밀번호만 허용한다")
        void acceptsOnlyOriginalPasswordAfterTemporaryPasswordExpires() {
            // given
            String email = "member@finditem.kr";
            User user = expiredTemporaryPasswordUser();
            PasswordVerifyRequest request = new PasswordVerifyRequest();
            request.setCurrentPassword("original-password");
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("original-password", "original-password-hash"))
                    .thenReturn(true);

            // when & then
            passwordService.verify(email, request);
        }

        @Test
        @DisplayName("임시 비밀번호가 만료되면 임시 비밀번호는 거절한다")
        void rejectsTemporaryPasswordAfterExpiry() {
            // given
            String email = "member@finditem.kr";
            User user = expiredTemporaryPasswordUser();
            PasswordVerifyRequest request = new PasswordVerifyRequest();
            request.setCurrentPassword("temporary-password");
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> passwordService.verify(email, request))
                    .isInstanceOfSatisfying(
                            com.fmi.global.apiPayload.exception.GeneralException.class,
                            exception -> assertThat(exception.getCode())
                                    .isEqualTo(com.fmi
                                            .global
                                            .apiPayload
                                            .code
                                            .status
                                            .ErrorStatus
                                            ._CURRENT_PASSWORD_INCORRECT));
            verify(passwordEncoder, never()).matches("temporary-password", "temporary-password-hash");
        }
    }

    private com.fmi.domain.auth.data.SocialAccounts mockSocialAccount() {
        return org.mockito.Mockito.mock(com.fmi.domain.auth.data.SocialAccounts.class);
    }

    private User expiredTemporaryPasswordUser() {
        return User.builder()
                .password("temporary-password-hash")
                .originalPassword("original-password-hash")
                .temporaryPassword("temporary-password-hash")
                .temporaryPasswordExpiresAt(LocalDateTime.of(2016, 8, 23, 3, 0))
                .build();
    }
}

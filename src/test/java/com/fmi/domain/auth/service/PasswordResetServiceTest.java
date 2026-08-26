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
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.repository.SocialAccountsRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAccountsRepository socialAccountsRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Nested
    @DisplayName("임시 비밀번호 발급")
    class IssueTemporaryPassword {

        @Nested
        @DisplayName("일반 계정이면")
        class WithLocalAccount {

            @Test
            @DisplayName("저장한 뒤 동기 메일을 발송한다")
            void sendsMailAfterSaving() {
                // given
                String email = "member@finditem.kr";
                User user = User.builder()
                        .email(email)
                        .password("original-password-hash")
                        .build();
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.empty());
                when(passwordEncoder.encode(anyString()))
                        .thenReturn("temporary-password-hash", "current-password-hash");

                // when
                passwordResetService.issueTemporaryPassword(email);

                // then
                InOrder order = inOrder(userRepository, emailService);
                order.verify(userRepository).save(user);
                order.verify(emailService)
                        .sendHtmlEmail(eq(email), eq("임시 비밀번호 발급"), eq("password-reset-email.html"), anyMap());
                assertThat(user.getOriginalPassword()).isEqualTo("original-password-hash");
                assertThat(user.getTemporaryPassword()).isEqualTo("temporary-password-hash");
                assertThat(user.getPassword()).isEqualTo("current-password-hash");
                assertThat(user.getTemporaryPasswordExpiresAt())
                        .isAfter(LocalDateTime.now().plusMinutes(59));
                assertThat(user.getTemporaryPasswordExpiresAt())
                        .isBefore(LocalDateTime.now().plusMinutes(61));
            }
        }

        @Nested
        @DisplayName("소셜 계정이면")
        class WithSocialAccount {

            @Test
            @DisplayName("저장과 메일 발송 없이 실패한다")
            void failsWithoutSavingOrSendingMail() {
                // given
                String email = "member@finditem.kr";
                User user = User.builder().email(email).build();
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.of(mockSocialAccount()));

                // when & then
                assertThatThrownBy(() -> passwordResetService.issueTemporaryPassword(email))
                        .isInstanceOf(RuntimeException.class);
                verify(userRepository, never()).save(any(User.class));
                verify(emailService, never()).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
            }
        }

        @Nested
        @DisplayName("메일 발송에 실패하면")
        class WithMailFailure {

            @Test
            @DisplayName("저장 이후 예외를 호출자에게 전파한다")
            void propagatesExceptionAfterSaving() {
                // given
                String email = "member@finditem.kr";
                User user = User.builder()
                        .email(email)
                        .password("original-password-hash")
                        .build();
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
                when(socialAccountsRepository.findByUser(user)).thenReturn(Optional.empty());
                when(passwordEncoder.encode(anyString()))
                        .thenReturn("temporary-password-hash", "current-password-hash");
                doThrow(new IllegalStateException("mail unavailable"))
                        .when(emailService)
                        .sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());

                // when & then
                assertThatThrownBy(() -> passwordResetService.issueTemporaryPassword(email))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("mail unavailable");
                InOrder order = inOrder(userRepository, emailService);
                order.verify(userRepository).save(user);
                order.verify(emailService).sendHtmlEmail(anyString(), anyString(), anyString(), anyMap());
            }
        }
    }

    private com.fmi.domain.auth.data.SocialAccounts mockSocialAccount() {
        return org.mockito.Mockito.mock(com.fmi.domain.auth.data.SocialAccounts.class);
    }
}

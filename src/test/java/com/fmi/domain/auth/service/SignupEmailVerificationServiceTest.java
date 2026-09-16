package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.service.internal.AuthEmailNotifier;
import com.fmi.domain.auth.service.internal.EmailVerificationStore;
import com.fmi.domain.auth.service.internal.EmailVerificationStore.VerificationCode;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.external.mail.EmailBounceRegistry;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignupEmailVerificationServiceTest {

    private static final String EMAIL = "member@finditem.kr";

    @Mock
    private EmailVerificationStore emailVerificationStore;

    @Mock
    private AuthEmailNotifier authEmailNotifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailBounceRegistry emailBounceRegistry;

    @InjectMocks
    private SignupEmailVerificationService signupEmailVerificationService;

    @Nested
    @DisplayName("인증 코드 발송")
    class SendCode {

        @Nested
        @DisplayName("가입 가능한 이메일이면")
        class WithAvailableEmail {

            @Test
            @DisplayName("새 인증 코드를 저장하고 메일 발송을 요청한다")
            void storesNewCodeAndRequestsEmail() {
                // given
                when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
                when(emailBounceRegistry.hasBounced(EMAIL)).thenReturn(false);

                // when
                signupEmailVerificationService.sendCode(EMAIL);

                // then
                var codeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
                var expiryCaptor = org.mockito.ArgumentCaptor.forClass(Instant.class);
                verify(emailVerificationStore).replaceCode(eq(EMAIL), codeCaptor.capture(), expiryCaptor.capture());
                verify(authEmailNotifier).sendVerificationCode(EMAIL, codeCaptor.getValue());
                assertThat(codeCaptor.getValue()).matches("\\d{6}");
                assertThat(expiryCaptor.getValue()).isAfter(Instant.now());
            }
        }
    }

    @Nested
    @DisplayName("인증 코드 검증")
    class Verify {

        @Nested
        @DisplayName("유효한 인증 코드가 있으면")
        class WithValidCode {

            @Test
            @DisplayName("기존 코드를 삭제하고 인증 완료 상태를 저장한다")
            void deletesCodeAndStoresVerifiedState() {
                // given
                User user = User.builder().email(EMAIL).email_verified(false).build();
                Instant expiresAt = Instant.now().plusSeconds(60);
                VerificationCode verificationCode = new VerificationCode("123456", expiresAt);
                when(emailVerificationStore.findCode(EMAIL)).thenReturn(Optional.of(verificationCode));
                when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

                // when
                signupEmailVerificationService.verify(EMAIL, "123456");

                // then
                verify(emailVerificationStore).deleteCode(EMAIL);
                verify(emailVerificationStore).markVerified(EMAIL);
                assertThat(user.isEmail_verified()).isTrue();
            }
        }
    }
}

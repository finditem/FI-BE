package com.fmi.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.auth.service.internal.PasswordValidator;
import com.fmi.domain.auth.web.dto.SignupRequest;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NicknameValidationService nicknameValidationService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private Clock clock;

    private final AtomicReference<User> savedUser = new AtomicReference<>();
    private PasswordValidator passwordValidator;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator(passwordEncoder, clock);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                nicknameValidationService,
                emailVerificationService,
                emailService,
                passwordValidator);
        lenient().when(userRepository.existsByEmail(anyString())).thenReturn(false);
        lenient()
                .when(userRepository.existsRecentlyDeletedByEmail(anyString(), any()))
                .thenReturn(false);
        lenient().when(emailVerificationService.isEmailVerified(anyString())).thenReturn(true);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            savedUser.set(user);
            return user;
        });
    }

    @Nested
    @DisplayName("회원 가입")
    class Signup {

        @Nested
        @DisplayName("이메일 인증 정보가 있으면")
        class WithEmailVerificationFlag {

            @Test
            @DisplayName("인증 정보를 소비한 뒤 이메일 인증 사용자를 저장한다")
            void 가입은_인증_flag를_소비한_뒤_이메일_인증_사용자를_저장한다() {
                // given
                SignupRequest request = signupRequest();

                // when
                User result = authService.signup(request);

                // then
                InOrder order = inOrder(emailVerificationService, userRepository);
                order.verify(emailVerificationService).isEmailVerified(request.getEmail());
                order.verify(emailVerificationService).consumeEmailVerification(request.getEmail());
                order.verify(userRepository).save(any(User.class));
                assertThat(result).isSameAs(savedUser.get());
                assertThat(savedUser.get())
                        .extracting(User::getEmail, User::getNickname, User::isEmail_verified)
                        .containsExactly(request.getEmail(), request.getNickname(), true);
                verify(emailService)
                        .sendHtmlEmailAsync(
                                eq(request.getEmail()), eq("회원가입을 환영합니다"), eq("welcome-email.html"), anyMap());
            }
        }

        @Nested
        @DisplayName("환영 메일 발송에 실패하면")
        class WithWelcomeMailFailure {

            @Test
            @DisplayName("가입은 성공한다")
            void 환영_메일_발송이_실패해도_가입은_성공한다() {
                // given
                SignupRequest request = signupRequest();
                doThrow(new IllegalStateException("mail unavailable"))
                        .when(emailService)
                        .sendHtmlEmailAsync(anyString(), anyString(), anyString(), anyMap());

                // when
                User result = authService.signup(request);

                // then
                assertThat(result).isSameAs(savedUser.get());
                verify(userRepository).save(any(User.class));
            }
        }
    }

    @Nested
    @DisplayName("로그인 인증")
    class Authenticate {

        @Test
        @DisplayName("임시 비밀번호가 만료되면 원래 비밀번호로 로그인한다")
        void authenticatesWithOriginalPasswordAfterTemporaryPasswordExpires() {
            // given
            String email = "member@finditem.kr";
            User user = expiredTemporaryPasswordUser();
            stubClock();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            lenient()
                    .when(passwordEncoder.matches("original-password", "original-password-hash"))
                    .thenReturn(true);

            // when
            AuthService.AuthenticateResult result = authService.authenticate(email, "original-password");

            // then
            assertThat(result.getUser()).isSameAs(user);
            assertThat(result.isTemporaryPassword()).isFalse();
        }

        @Test
        @DisplayName("임시 비밀번호가 만료되면 임시 비밀번호 로그인을 거절한다")
        void rejectsTemporaryPasswordAfterExpiry() {
            // given
            String email = "member@finditem.kr";
            User user = expiredTemporaryPasswordUser();
            stubClock();
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            lenient()
                    .when(passwordEncoder.matches("temporary-password", "temporary-password-hash"))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.authenticate(email, "temporary-password"))
                    .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                            .isEqualTo(ErrorStatus._INVALID_CREDENTIALS));
        }
    }

    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        request.setEmail("member@finditem.kr");
        request.setPassword("Password1!");
        request.setNickname("찾아줘토끼1");
        request.setPrivacyPolicyAgreed(true);
        request.setTermsOfServiceAgreed(true);
        request.setContentPolicyAgreed(false);
        request.setMarketingConsent(false);
        return request;
    }

    private User expiredTemporaryPasswordUser() {
        return User.builder()
                .password("temporary-password-hash")
                .originalPassword("original-password-hash")
                .temporaryPassword("temporary-password-hash")
                .temporaryPasswordExpiresAt(LocalDateTime.of(2016, 8, 23, 3, 0))
                .build();
    }

    private void stubClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-08-23T03:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }
}

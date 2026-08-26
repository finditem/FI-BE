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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.admin.web.dto.AdminSignupRequest;
import com.fmi.domain.auth.service.internal.PasswordValidator;
import com.fmi.domain.auth.service.internal.SignupValidator;
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
@DisplayName("AuthService")
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

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-23T03:00:00Z"), ZoneOffset.UTC);

    private final AtomicReference<User> savedUser = new AtomicReference<>();
    private PasswordValidator passwordValidator;
    private SignupValidator signupValidator;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordValidator = new PasswordValidator(passwordEncoder, clock);
        signupValidator = new SignupValidator(userRepository, clock);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                nicknameValidationService,
                emailVerificationService,
                emailService,
                passwordValidator,
                signupValidator);
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
    @DisplayName("회원 가입할 때")
    class DescribeSignup {

        @Nested
        @DisplayName("이메일 인증 정보가 있으면")
        class ContextWithEmailVerificationFlag {

            @Test
            @DisplayName("인증 정보를 확인한 뒤 이메일 인증 사용자를 저장한다")
            void itSavesVerifiedUserAfterConsumingVerification() {
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
        class ContextWithWelcomeMailFailure {

            @Test
            @DisplayName("사용자를 저장한다")
            void itSavesUser() {
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

        @Nested
        @DisplayName("비밀번호 정책을 만족하지 않으면")
        class ContextWithWeakPassword {

            @Test
            @DisplayName("인코딩과 이메일 인증 확인 없이 기존 약한 비밀번호 예외를 던진다")
            void itThrowsWeakPasswordBeforeEncodingAndVerification() {
                // given
                SignupRequest request = signupRequest();
                request.setPassword("short");

                // when & then
                assertThatThrownBy(() -> authService.signup(request))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._WEAK_PASSWORD));
                verify(emailVerificationService, never()).isEmailVerified(request.getEmail());
                verify(emailVerificationService, never()).consumeEmailVerification(request.getEmail());
                verify(passwordEncoder, never()).encode(request.getPassword());
                verify(userRepository, never()).save(any(User.class));
            }
        }
    }

    @Nested
    @DisplayName("관리자 회원 가입할 때")
    class DescribeAdminSignup {

        @Nested
        @DisplayName("유효한 가입 요청이면")
        class ContextWithValidSignupRequest {

            @Test
            @DisplayName("관리자 상태의 사용자를 저장한다")
            void itSavesAdminUser() {
                // given
                AdminSignupRequest request = adminSignupRequest();

                // when
                authService.adminSignup(request);

                // then
                assertThat(savedUser.get())
                        .extracting(User::getEmail, User::getNickname, User::getPassword, User::getRole)
                        .containsExactly(request.getEmail(), request.getNickname(), "encoded-password", Role.ADMIN);
                assertThat(savedUser.get().isPrivacyPolicyAgreed()).isFalse();
                assertThat(savedUser.get().isTermsOfServiceAgreed()).isFalse();
                assertThat(savedUser.get().isContentPolicyAgreed()).isFalse();
                assertThat(savedUser.get().isMarketingConsent()).isFalse();
            }
        }

        @Nested
        @DisplayName("비밀번호 정책을 만족하지 않으면")
        class ContextWithWeakPassword {

            @Test
            @DisplayName("인코딩과 저장 없이 기존 약한 비밀번호 예외를 던진다")
            void itThrowsWeakPasswordBeforeEncodingAndSaving() {
                // given
                AdminSignupRequest request = adminSignupRequest();
                request.setPassword("short");

                // when & then
                assertThatThrownBy(() -> authService.adminSignup(request))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._WEAK_PASSWORD));
                verify(passwordEncoder, never()).encode(request.getPassword());
                verify(userRepository, never()).save(any(User.class));
            }
        }
    }

    @Nested
    @DisplayName("로그인을 인증할 때")
    class DescribeAuthenticate {

        @Nested
        @DisplayName("임시 비밀번호가 만료되고 원래 비밀번호가 일치하면")
        class ContextWithExpiredTemporaryPasswordAndMatchingOriginalPassword {

            @Test
            @DisplayName("일반 인증 결과를 반환한다")
            void itReturnsStandardAuthentication() {
                // given
                String email = "member@finditem.kr";
                User user = expiredTemporaryPasswordUser();
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
        }

        @Nested
        @DisplayName("임시 비밀번호가 만료된 상태에서 임시 비밀번호를 입력하면")
        class ContextWithExpiredTemporaryPasswordAndTemporaryPassword {

            @Test
            @DisplayName("유효하지 않은 자격 증명 예외를 던진다")
            void itThrowsInvalidCredentials() {
                // given
                String email = "member@finditem.kr";
                User user = expiredTemporaryPasswordUser();
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

    private AdminSignupRequest adminSignupRequest() {
        AdminSignupRequest request = new AdminSignupRequest();
        request.setEmail("admin@finditem.kr");
        request.setNickname("admin");
        request.setPassword("Password1!");
        request.setEmailVerified(true);
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
}

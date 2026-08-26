package com.fmi.domain.auth.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.fmi.domain.auth.event.UserSignedUpEvent;
import com.fmi.domain.auth.service.EmailVerificationService;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignupEventHandler")
class SignupEventHandlerTest {

    private static final String EMAIL = "member@finditem.kr";
    private static final LocalDateTime SIGNED_UP_AT = LocalDateTime.of(2026, 8, 26, 15, 0);

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<Map<String, String>> variablesCaptor;

    private SignupEventHandler signupEventHandler;

    @BeforeEach
    void setUp() {
        signupEventHandler = new SignupEventHandler(emailVerificationService, emailService);
    }

    @Nested
    @DisplayName("가입 완료 이벤트를 처리할 때")
    class DescribeHandle {

        @Test
        @DisplayName("인증 플래그를 소비하고 환영 메일 발송을 요청한다")
        void itConsumesVerificationAndSendsWelcomeEmail() {
            // given
            UserSignedUpEvent event = new UserSignedUpEvent(1L, EMAIL, "찾아줘토끼", SIGNED_UP_AT);

            // when
            signupEventHandler.handle(event);

            // then
            InOrder order = inOrder(emailVerificationService, emailService);
            order.verify(emailVerificationService).consumeEmailVerification(EMAIL);
            order.verify(emailService)
                    .sendHtmlEmailAsync(
                            eq(EMAIL), eq("회원가입을 환영합니다"), eq("welcome-email.html"), variablesCaptor.capture());
            assertThat(variablesCaptor.getValue())
                    .containsEntry("NAME", "찾아줘토끼")
                    .containsEntry("USER", EMAIL)
                    .containsEntry("DATE", "2026년 08월 26일");
        }

        @Nested
        @DisplayName("환영 메일 발송에 실패하면")
        class ContextWithWelcomeEmailFailure {

            @Test
            @DisplayName("예외를 전파하지 않는다")
            void itDoesNotPropagateException() {
                // given
                UserSignedUpEvent event = new UserSignedUpEvent(1L, EMAIL, "찾아줘토끼", SIGNED_UP_AT);
                doThrow(new IllegalStateException("mail unavailable"))
                        .when(emailService)
                        .sendHtmlEmailAsync(eq(EMAIL), eq("회원가입을 환영합니다"), eq("welcome-email.html"), anyMap());

                // when & then
                assertThatCode(() -> signupEventHandler.handle(event)).doesNotThrowAnyException();
                verify(emailVerificationService).consumeEmailVerification(EMAIL);
            }
        }
    }
}

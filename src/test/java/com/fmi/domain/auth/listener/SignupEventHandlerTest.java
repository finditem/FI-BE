package com.fmi.domain.auth.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.fmi.domain.auth.event.UserSignedUpEvent;
import com.fmi.domain.auth.service.EmailVerificationService;
import com.fmi.domain.auth.service.internal.AuthEmailNotifier;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private AuthEmailNotifier authEmailNotifier;

    private SignupEventHandler signupEventHandler;

    @BeforeEach
    void setUp() {
        signupEventHandler = new SignupEventHandler(emailVerificationService, authEmailNotifier);
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
            InOrder order = inOrder(emailVerificationService, authEmailNotifier);
            order.verify(emailVerificationService).consumeEmailVerification(EMAIL);
            order.verify(authEmailNotifier).sendSignupWelcome(EMAIL, "찾아줘토끼", SIGNED_UP_AT);
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
                        .when(authEmailNotifier)
                        .sendSignupWelcome(EMAIL, "찾아줘토끼", SIGNED_UP_AT);

                // when & then
                assertThatCode(() -> signupEventHandler.handle(event)).doesNotThrowAnyException();
                verify(emailVerificationService).consumeEmailVerification(EMAIL);
            }
        }
    }
}

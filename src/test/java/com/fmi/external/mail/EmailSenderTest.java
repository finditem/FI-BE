package com.fmi.external.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailSender")
class EmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateRenderer templateRenderer;

    @InjectMocks
    private EmailSender emailSender;

    private final Logger logger = (Logger) LoggerFactory.getLogger(EmailSender.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void attachLogAppender() {
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        logger.detachAppender(logAppender);
    }

    @Nested
    @DisplayName("이메일을 발송할 때")
    class DescribeSend {

        @Nested
        @DisplayName("SMTP 발송이 실패하면")
        class ContextWithSmtpFailure {

            @Test
            @DisplayName("원인 예외를 ERROR 로그로 남기고 _EMAIL_SEND_FAILED를 던진다")
            void itLogsCauseAndThrowsEmailSendFailed() {
                // given
                when(templateRenderer.templateExists("verify-code.html")).thenReturn(false);
                doThrow(new MailSendException("Read timed out"))
                        .when(mailSender)
                        .send(any(SimpleMailMessage.class));

                // when & then
                assertThatThrownBy(() -> emailSender.send(
                                "member@finditem.kr", "이메일 인증 코드", "verify-code.html", Map.of("code", "123456")))
                        .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getCode())
                                .isEqualTo(ErrorStatus._EMAIL_SEND_FAILED));
                assertThat(logAppender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage())
                            .isEqualTo("이메일 발송 실패: subject=이메일 인증 코드")
                            .doesNotContain("member@finditem.kr");
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Read timed out");
                });
            }
        }
    }

    @Nested
    @DisplayName("이메일을 비동기로 발송할 때")
    class DescribeSendAsync {

        @Nested
        @DisplayName("SMTP 발송이 실패하면")
        class ContextWithSmtpFailure {

            @Test
            @DisplayName("예외를 호출자에게 전달하지 않고 원인 예외를 ERROR 로그로 남긴다")
            void itLogsCauseWithoutThrowing() {
                // given
                when(templateRenderer.templateExists("verify-code.html")).thenReturn(false);
                doThrow(new MailSendException("Connect timed out"))
                        .when(mailSender)
                        .send(any(SimpleMailMessage.class));

                // when & then
                assertThatCode(() -> emailSender.sendAsync(
                                "member@finditem.kr", "이메일 인증 코드", "verify-code.html", Map.of("code", "123456")))
                        .doesNotThrowAnyException();
                assertThat(logAppender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("Connect timed out");
                });
            }
        }
    }
}

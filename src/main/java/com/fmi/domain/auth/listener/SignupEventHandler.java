package com.fmi.domain.auth.listener;

import com.fmi.domain.auth.event.UserSignedUpEvent;
import com.fmi.domain.auth.service.EmailVerificationService;
import com.fmi.service.EmailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignupEventHandler {

    private static final DateTimeFormatter SIGNUP_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSignedUpEvent event) {
        emailVerificationService.consumeEmailVerification(event.email());

        try {
            emailService.sendHtmlEmailAsync(
                    event.email(),
                    "회원가입을 환영합니다",
                    "welcome-email.html",
                    Map.of(
                            "NAME", event.nickname() != null ? event.nickname() : "회원",
                            "USER", event.email(),
                            "DATE",
                                    SIGNUP_DATE_FORMATTER.format(
                                            event.signedUpAt() != null ? event.signedUpAt() : LocalDateTime.now())));
        } catch (Exception e) {
            log.warn("회원가입 환영 이메일 발송 실패: email={}", event.email(), e);
        }
    }
}

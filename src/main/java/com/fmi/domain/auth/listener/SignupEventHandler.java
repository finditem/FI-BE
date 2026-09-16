package com.fmi.domain.auth.listener;

import com.fmi.domain.auth.event.UserSignedUpEvent;
import com.fmi.domain.auth.service.SignupEmailVerificationService;
import com.fmi.domain.auth.service.internal.AuthEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignupEventHandler {

    private final SignupEmailVerificationService signupEmailVerificationService;
    private final AuthEmailNotifier authEmailNotifier;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(UserSignedUpEvent event) {
        signupEmailVerificationService.consumeEmailVerification(event.email());

        try {
            authEmailNotifier.sendSignupWelcome(event.email(), event.nickname(), event.signedUpAt());
        } catch (Exception e) {
            log.warn("회원가입 환영 이메일 발송 실패: email={}", event.email(), e);
        }
    }
}

package com.fmi.domain.auth.service.internal;

import com.fmi.external.mail.EmailSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEmailNotifier {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final EmailSender emailSender;

    public void sendVerificationCode(String email, String code) {
        emailSender.sendAsync(email, "이메일 인증 코드", "verify-code.html", Map.of("CODE", code));
    }

    public void sendTemporaryPassword(String email, String temporaryPassword) {
        emailSender.send(email, "임시 비밀번호 발급", "password-reset-email.html", Map.of("PASSWORD", temporaryPassword));
    }

    public void sendSignupWelcome(String email, String nickname, LocalDateTime signedUpAt) {
        LocalDateTime effectiveSignedUpAt = signedUpAt != null ? signedUpAt : LocalDateTime.now();
        emailSender.sendAsync(
                email,
                "회원가입을 환영합니다",
                "welcome-email.html",
                Map.of(
                        "NAME",
                        nickname != null ? nickname : "회원",
                        "USER",
                        email,
                        "DATE",
                        DATE_FORMATTER.format(effectiveSignedUpAt)));
    }

    public void sendWithdrawal(String email, String nickname, LocalDateTime withdrawnAt) {
        emailSender.sendAsync(
                email,
                "계정이 삭제되었습니다",
                "account-deletion-email.html",
                Map.of(
                        "NAME",
                        nickname != null ? nickname : "회원",
                        "USER",
                        email,
                        "DATE",
                        DATE_FORMATTER.format(withdrawnAt)));
    }
}

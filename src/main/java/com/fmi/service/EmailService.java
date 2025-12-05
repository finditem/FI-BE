package com.fmi.service;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:test@example.com}")
    private String fromEmail;
    
    @Value("${spring.mail.test-mode:false}")
    private boolean testMode;

    public void sendEmail(String to, String subject, String body) {
        // 테스트 모드: 실제 발송하지 않고 로그만 출력
        if (testMode) {
            log.info("📧 [EMAIL TEST MODE] to={}, subject={}, body={}", to, subject, body);
            log.info("✅ 실제 이메일은 발송되지 않았습니다 (테스트 모드)");
            return;
        }
        
        // 실제 이메일 발송
        try {
            log.debug("📧 [EMAIL SEND] from={}, to={}, subject={}", fromEmail, to, subject);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("✅ [EMAIL SENT] to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("❌ [EMAIL FAILED] from={}, to={}, error={}", fromEmail, to, e.getMessage(), e);
            log.error("💡 Tip: spring.mail.username/password 환경 변수를 확인하세요. 현재 fromEmail={}", fromEmail);
            // 이메일 발송 실패 시 GeneralException을 던져서 API가 실패 응답을 반환하도록 함
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        }
    }
}



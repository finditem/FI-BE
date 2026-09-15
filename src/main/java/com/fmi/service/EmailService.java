package com.fmi.service;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${spring.mail.username:test@example.com}")
    private String fromEmail;

    @Value("${spring.mail.test-mode:false}")
    private boolean testMode;

    /**
     * 텍스트 이메일 발송 (기존 메서드 - 하위 호환성 유지)
     */
    public void sendEmail(String to, String subject, String body) {
        if (testMode) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
        } catch (Exception ignored) {
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        }
    }

    /**
     * HTML 이메일 비동기 발송 (API 응답 블로킹 없음)
     */
    @Async
    public void sendHtmlEmailAsync(String to, String subject, String templateName, Map<String, String> variables) {
        try {
            sendHtmlEmail(to, subject, templateName, variables);
        } catch (Exception ignored) {
        }
    }

    /**
     * HTML 이메일 발송 (템플릿 사용)
     *
     * @param to           수신자 이메일
     * @param subject      이메일 제목
     * @param templateName 템플릿 파일명 (예: "verify-code.html")
     * @param variables    템플릿 변수 맵 (예: {"code": "123456", "name": "홍길동"})
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, String> variables) {
        if (testMode) {
            return;
        }

        boolean templateExists = templateService.templateExists(templateName);
        if (!templateExists) {
            String textBody = variables != null && variables.containsKey("code")
                    ? "인증번호: " + variables.get("code")
                    : "이메일이 발송되었습니다.";
            sendEmail(to, subject, textBody);
            return;
        }

        try {
            String htmlContent = templateService.loadTemplate(templateName, variables);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException ignored) {
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        } catch (Exception ignored) {
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        }
    }
}

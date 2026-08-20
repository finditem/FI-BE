package com.fmi.service;

import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
        // 테스트 모드: 실제 발송하지 않고 로그만 출력
        if (testMode) {
            log.info("[EMAIL TEST MODE] to={}, subject={}, body={}", to, subject, body);
            log.info("실제 이메일은 발송되지 않았습니다 (테스트 모드)");
            return;
        }

        // 실제 이메일 발송
        try {
            log.debug("[EMAIL SEND] from={}, to={}, subject={}", fromEmail, to, subject);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("[EMAIL SEND REQUEST] to={} subject={} (SMTP 서버에 발송 요청 완료, 실제 수신 여부는 나중에 확인됨)", to, subject);
        } catch (Exception e) {
            log.error("[EMAIL FAILED] from={}, to={}, error={}", fromEmail, to, e.getMessage(), e);
            log.error("Tip: spring.mail.username/password 환경 변수를 확인하세요. 현재 fromEmail={}", fromEmail);
            // 이메일 발송 실패 시 GeneralException을 던져서 API가 실패 응답을 반환하도록 함
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
        } catch (Exception e) {
            log.error("[EMAIL ASYNC FAILED] to={}, template={}, error={}", to, templateName, e.getMessage(), e);
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
        // 템플릿 존재 여부 확인 (테스트 모드에서도 확인)
        boolean templateExists = templateService.templateExists(templateName);
        if (!templateExists) {
            log.warn("[EMAIL TEMPLATE] 템플릿 파일이 존재하지 않습니다: {}. 텍스트 이메일로 대체합니다.", templateName);
            log.warn("[EMAIL TEMPLATE] 예상 경로: src/main/resources/templates/emails/{}", templateName);
            // 템플릿이 없으면 기본 텍스트로 대체
            String textBody = variables != null && variables.containsKey("code")
                    ? "인증번호: " + variables.get("code")
                    : "이메일이 발송되었습니다.";
            if (testMode) {
                log.info("[EMAIL TEST MODE] to={}, subject={}, body={}", to, subject, textBody);
                log.info("실제 이메일은 발송되지 않았습니다 (테스트 모드)");
            } else {
                sendEmail(to, subject, textBody);
            }
            return;
        }

        // 테스트 모드: 실제 발송하지 않고 로그만 출력
        if (testMode) {
            log.info("[EMAIL TEST MODE - HTML] to={}, subject={}, template={}", to, subject, templateName);
            log.info("[EMAIL TEST MODE] variables={}", variables);
            log.info("실제 이메일은 발송되지 않았습니다 (테스트 모드)");

            // 템플릿 로드 테스트 (변수 치환 확인용)
            try {
                String htmlContent = templateService.loadTemplate(templateName, variables);
                log.info("[EMAIL TEMPLATE] 템플릿 로드 및 변수 치환 성공 (길이: {} bytes)", htmlContent.length());
            } catch (Exception e) {
                log.error("[EMAIL TEMPLATE] 템플릿 로드 실패: {}", e.getMessage());
            }
            return;
        }

        try {
            // 템플릿 로드 및 변수 치환
            String htmlContent = templateService.loadTemplate(templateName, variables);

            // HTML 이메일 생성
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML 형식

            mailSender.send(message);
            log.info(
                    "[EMAIL SEND REQUEST - HTML] to={} subject={} template={} (SMTP 서버에 발송 요청 완료)",
                    to,
                    subject,
                    templateName);

        } catch (MessagingException e) {
            log.error(
                    "[EMAIL FAILED - HTML] from={}, to={}, template={}, error={}",
                    fromEmail,
                    to,
                    templateName,
                    e.getMessage(),
                    e);
            log.error("Tip: spring.mail.username/password 환경 변수를 확인하세요. 현재 fromEmail={}", fromEmail);
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        } catch (Exception e) {
            log.error(
                    " [EMAIL FAILED - HTML] from={}, to={}, template={}, error={}",
                    fromEmail,
                    to,
                    templateName,
                    e.getMessage(),
                    e);
            throw new GeneralException(ErrorStatus._EMAIL_SEND_FAILED);
        }
    }
}

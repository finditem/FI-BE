package com.fmi.domain.report.service.internal;

import com.fmi.domain.report.data.Report;
import com.fmi.domain.user.data.User;
import com.fmi.external.mail.EmailSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportEmailNotifier {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final EmailSender emailSender;

    public void sendReceipt(Report report, User reporter, String targetTitle) {
        LocalDateTime createdAt = report.getCreatedAt() != null ? report.getCreatedAt() : LocalDateTime.now();
        emailSender.sendAsync(
                reporter.getEmail(),
                "신고가 접수되었습니다",
                "report-received-email.html",
                Map.of(
                        "NAME",
                        reporter.getNickname() != null ? reporter.getNickname() : "회원",
                        "TITLE",
                        targetTitle,
                        "USER",
                        reporter.getEmail(),
                        "DATE",
                        DATE_FORMATTER.format(createdAt),
                        "CONTENT",
                        report.getReason() != null ? report.getReason() : ""));
    }

    public void sendResult(Report report, User reporter, String targetTitle, String answer) {
        LocalDateTime createdAt = report.getCreatedAt() != null ? report.getCreatedAt() : LocalDateTime.now();
        emailSender.sendAsync(
                reporter.getEmail(),
                "신고 답변 안내",
                "report-result-email.html",
                Map.of(
                        "name",
                        reporter.getNickname() != null ? reporter.getNickname() : "회원",
                        "TITLE",
                        targetTitle,
                        "USER",
                        reporter.getEmail(),
                        "RESULT",
                        "답변 완료",
                        "DATE",
                        DATE_FORMATTER.format(createdAt),
                        "CONTENT",
                        answer));
    }

    public void sendAction(User targetUser, String categoryName) {
        String nickname = targetUser.getNickname() != null ? targetUser.getNickname() : "회원";
        emailSender.sendAsync(
                targetUser.getEmail(),
                "신고 처리 결과 안내",
                "report-notification-email.html",
                Map.of(
                        "name", nickname,
                        "CATEGORY", categoryName,
                        "USER", targetUser.getEmail(),
                        "NICKNAME", nickname));
    }
}

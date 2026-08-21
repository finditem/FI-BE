package com.fmi.global.service;

import com.fmi.domain.inquiry.event.InquiryEvent;
import com.fmi.domain.report.event.ReportEvent;
import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.slack.api.webhook.Payload;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SlackService {

    private final Slack slack = Slack.getInstance();

    @Value("${slack.webhook.inquiry-url}")
    private String inquiryWebhookUrl;

    @Value("${slack.webhook.report-url}")
    private String reportWebhookUrl;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void sendInquiryNotification(InquiryEvent event) {
        try {
            Payload payload = Payload.builder()
                    .text("🔔 *새로운 1:1 문의가 등록되었습니다!*")
                    .attachments(List.of(Attachment.builder()
                            .color("#36a64f")
                            .fields(List.of(
                                    new Field("문의 제목", event.title(), false),
                                    new Field("문의 타입", event.inquiryTypeDescription(), false),
                                    new Field("작성자(이메일)", event.email(), false),
                                    new Field("작성일시", format(event.createdAt()), false),
                                    new Field("문의 내용", event.content(), false)))
                            .build()))
                    .build();

            slack.send(inquiryWebhookUrl, payload);
        } catch (Exception e) {
            log.error("슬랙 문의 알림 전송 실패", e);
        }
    }

    public void sendReportNotification(ReportEvent event) {
        try {
            Payload payload = Payload.builder()
                    .text("🚨 *신고가 접수되었습니다!*")
                    .attachments(List.of(Attachment.builder()
                            .color("#d40e0d")
                            .fields(List.of(
                                    new Field(
                                            "신고 유형", "⚠️ " + event.reportType().getDescription(), false),
                                    new Field(
                                            "신고 항목",
                                            "📍 " + event.targetType().getDescription() + " (id=" + event.targetId()
                                                    + ")",
                                            false),
                                    new Field("사유", normalizeReason(event.reason()), false),
                                    new Field(
                                            "신고자",
                                            event.reporterNickname() + " (id=" + event.reporterId() + ")",
                                            false),
                                    new Field("접수 시각", format(event.createdAt()), false)))
                            .build()))
                    .build();

            slack.send(reportWebhookUrl, payload);
        } catch (Exception e) {
            log.error("슬랙 신고 알림 전송 실패. reportId={}", event.reportId(), e);
        }
    }

    private String normalizeReason(String reason) {
        return (reason == null || reason.isBlank()) ? "(사유 없음)" : reason;
    }

    private String format(LocalDateTime time) {
        return (time == null) ? "-" : time.format(DATE_FORMAT);
    }
}

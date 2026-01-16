package com.fmi.global.service;

import com.fmi.domain.inquiry.event.InquiryEvent;
import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.slack.api.webhook.Payload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SlackService {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    public void sendInquiryNotification(InquiryEvent event) {
        try {
            Slack slack = Slack.getInstance();

            Payload payload = Payload.builder()
                    .text("🔔 *새로운 1:1 문의가 등록되었습니다!*")
                    .attachments(List.of(Attachment.builder()
                            .color("#36a64f")
                            .fields(List.of(
                                    new Field("문의 제목", event.title(), false),
                                    new Field("카테고리", "📍 " + event.categoryDescription(), true),
                                    new Field("작성자(이메일)", event.email(), true),
                                    new Field("문의 내용", event.content(), false),
                                    new Field("작성일시", event.createdAt().toString(), true)
                            )).build()))
                    .build();

            slack.send(webhookUrl, payload);
        } catch (Exception e) {
            log.error("슬랙 알림 전송 실패", e);
        }
    }
}

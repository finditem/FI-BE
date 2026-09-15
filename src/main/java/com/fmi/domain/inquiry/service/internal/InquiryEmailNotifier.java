package com.fmi.domain.inquiry.service.internal;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.external.mail.EmailSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryEmailNotifier {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final EmailSender emailSender;

    public void sendReceipt(Inquiry inquiry) {
        String recipientEmail = inquiry.getEmail() != null
                ? inquiry.getEmail()
                : inquiry.getUser() != null ? inquiry.getUser().getEmail() : null;
        if (recipientEmail == null) {
            return;
        }

        String recipientName = inquiry.getUser() != null && inquiry.getUser().getNickname() != null
                ? inquiry.getUser().getNickname()
                : recipientEmail;
        LocalDateTime createdAt = inquiry.getCreatedAt() != null ? inquiry.getCreatedAt() : LocalDateTime.now();

        emailSender.sendAsync(
                recipientEmail,
                "문의가 접수되었습니다",
                "support-request-email.html",
                Map.of(
                        "name", recipientName,
                        "TITLE", inquiry.getTitle(),
                        "DATE", DATE_FORMATTER.format(createdAt),
                        "CONTENT", inquiry.getContent() != null ? inquiry.getContent() : "",
                        "INQUIRY_ID", String.valueOf(inquiry.getId())));
    }

    public void sendGuestReply(Inquiry inquiry, String content) {
        emailSender.sendAsync(
                inquiry.getEmail(),
                "문의에 대한 답변이 도착했습니다",
                "support-reply-email.html",
                Map.of(
                        "name", inquiry.getEmail(),
                        "TITLE", inquiry.getTitle(),
                        "DATE", DATE_FORMATTER.format(LocalDateTime.now()),
                        "CONTENT", content,
                        "INQUIRY_ID", String.valueOf(inquiry.getId())));
    }
}

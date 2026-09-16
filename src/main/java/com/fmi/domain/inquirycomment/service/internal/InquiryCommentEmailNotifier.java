package com.fmi.domain.inquirycomment.service.internal;

import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquirycomment.data.InquiryComment;
import com.fmi.external.mail.EmailSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InquiryCommentEmailNotifier {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    private final EmailSender emailSender;

    public void sendReply(Inquiry inquiry, InquiryComment comment) {
        String nickname =
                inquiry.getUser().getNickname() != null ? inquiry.getUser().getNickname() : "회원";
        emailSender.sendAsync(
                inquiry.getUser().getEmail(),
                "문의에 대한 답변이 도착했습니다",
                "support-reply-email.html",
                Map.of(
                        "name", nickname,
                        "TITLE", inquiry.getTitle(),
                        "DATE", DATE_FORMATTER.format(LocalDateTime.now()),
                        "CONTENT", comment.getContent(),
                        "INQUIRY_ID", String.valueOf(inquiry.getId())));
    }
}

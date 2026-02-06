package com.fmi.domain.inquiry.event;

import com.fmi.domain.inquiry.data.Inquiry;

import java.time.LocalDateTime;

public record InquiryEvent(
        String title,
        String content,
        String inquiryTypeDescription,
        String email,
        LocalDateTime createdAt
) {
    public static InquiryEvent from(Inquiry inquiry) {
        String categoryLabel = inquiry.getInquiryType().getDescription();

        String resolvedEmail = (inquiry.getUser() != null)
                ? inquiry.getUser().getEmail()
                : inquiry.getEmail();

        return new InquiryEvent(
                inquiry.getTitle(),
                inquiry.getContent(),
                categoryLabel,
                resolvedEmail,
                inquiry.getCreatedAt() != null ? inquiry.getCreatedAt() : LocalDateTime.now()
        );
    }

}

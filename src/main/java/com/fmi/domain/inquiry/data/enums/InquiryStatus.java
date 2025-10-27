package com.fmi.domain.inquiry.data.enums;

import lombok.Getter;

@Getter
public enum InquiryStatus {
    PENDING("답변 대기 중"),
    ANSWERED("답변 완료"),
    CLOSED("문의 종료");

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }
}


package com.fmi.domain.inquiry.data.enums;

import lombok.Getter;

@Getter
public enum InquiryStatus {
    RECEIVED("접수 대기"),
    PENDING("처리 중"),
    ANSWERED("답변 완료");

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }
}


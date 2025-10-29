package com.fmi.domain.inquiry.data.enums;

import lombok.Getter;

@Getter
public enum InquiryStatus {
    RECEIVED("접수"),
    PENDING("보류"),
    ANSWERED("답변완료");

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }
}


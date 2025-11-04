package com.fmi.domain.inquiry.data.enums;

import lombok.Getter;

@Getter
public enum InquiryType {
    PUBLIC("전체 공개 문의"),
    PRIVATE("1:1 개인 문의");

    private final String description;

    InquiryType(String description) {
        this.description = description;
    }
}


package com.fmi.domain.inquiry.data.enums;

import lombok.Getter;

@Getter
public enum InquiryType {
    GENERAL("일반 문의"),
    TECHNICAL("기술 지원"),
    ACCOUNT("계정 문의"),
    PAYMENT("결제/환불"),
    REPORT_ISSUE("신고 관련"),
    SERVICE("서비스 문의"),
    ETC("기타");

    private final String description;

    InquiryType(String description) {
        this.description = description;
    }
}


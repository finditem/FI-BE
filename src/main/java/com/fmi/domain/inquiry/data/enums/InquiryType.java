package com.fmi.domain.inquiry.data.enums;

import lombok.Getter;

@Getter
public enum InquiryType {
    ACCOUNT_LOGIN("계정/로그인"),
    USAGE("이용 문의"),
    BUG("버그/오류 신고"),
    SUGGESTION("제안/피드백"),
    ETC("기타");

    private final String description;

    InquiryType(String description) {
        this.description = description;
    }
}


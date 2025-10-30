package com.fmi.domain.faq.data.enums;

import lombok.Getter;

@Getter
public enum FaqCategory {
    USAGE("이용 방법"),
    ACCOUNT("계정 관리"),
    PAYMENT("결제/환불"),
    REPORT("신고 관련"),
    TECHNICAL("기술 지원"),
    ETC("기타");

    private final String description;

    FaqCategory(String description) {
        this.description = description;
    }
}


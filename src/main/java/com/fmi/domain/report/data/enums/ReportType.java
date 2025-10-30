package com.fmi.domain.report.data.enums;

import lombok.Getter;

@Getter
public enum ReportType {
    FRAUD("허위/사기"),
    SPAM("스팸/광고"),
    INAPPROPRIATE("부적절한 콘텐츠"),
    ABUSE("욕설/비방"),
    HARASSMENT("괴롭힘"),
    VIOLENCE("폭력적 콘텐츠"),
    ADULT("음란물"),
    PRIVACY("개인정보 노출"),
    COPYRIGHT("저작권 침해"),
    ETC("기타");

    private final String description;

    ReportType(String description) {
        this.description = description;
    }
}


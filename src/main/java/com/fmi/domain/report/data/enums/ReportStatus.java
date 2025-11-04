package com.fmi.domain.report.data.enums;

import lombok.Getter;

@Getter
public enum ReportStatus {
    PENDING("접수 대기"),
    REVIEWED("검토 중"),
    RESOLVED("처리 완료"),
    REJECTED("반려");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }
}


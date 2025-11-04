package com.fmi.domain.report.data.enums;

import lombok.Getter;

@Getter
public enum ReportTargetType {
    POST("게시글"),
    COMMENT("댓글"),
    USER("사용자"),
    CHAT("채팅");

    private final String description;

    ReportTargetType(String description) {
        this.description = description;
    }
}


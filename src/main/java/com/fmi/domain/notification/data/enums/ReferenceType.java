package com.fmi.domain.notification.data.enums;

import lombok.Getter;

@Getter
public enum ReferenceType {
    POST("게시글"),
    COMMENT("댓글"),
    CHAT("채팅"),
    INQUIRY("문의"),
    NOTICE("공지사항"),
    REPORT("신고");

    private final String description;

    ReferenceType(String description) {
        this.description = description;
    }
}


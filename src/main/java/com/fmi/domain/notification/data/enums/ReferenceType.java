package com.fmi.domain.notification.data.enums;

import lombok.Getter;

@Getter
public enum ReferenceType {
    CHAT("채팅"),
    POST("게시글"),
    NOTICE("공지"),
    COMMENT("댓글"),
    INQUIRY("문의"),
    REPORT("신고");

    private final String description;

    ReferenceType(String description) {
        this.description = description;
    }

}

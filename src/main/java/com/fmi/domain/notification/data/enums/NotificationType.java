package com.fmi.domain.notification.data.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    COMMENT("댓글"),
    REPLY("대댓글"),
    CHAT("채팅"),
    INQUIRY_REPLY("문의 답변"),
    REPORT_RESULT("신고 처리 결과"),
    FAVORITE("좋아요"),
    KEYWORD("키워드"),
    NOTICE("공지사항"),
    SYSTEM("시스템");


    private final String description;

    NotificationType(String description) {
        this.description = description;
    }
}


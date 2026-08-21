package com.fmi.domain.notice.data.enums;

import lombok.Getter;

@Getter
public enum NoticeCategory {
    GENERAL("일반 공지"),
    EVENT("이벤트"),
    MAINTENANCE("점검"),
    IMPORTANT("중요 공지"),
    UPDATE("업데이트");

    private final String description;

    NoticeCategory(String description) {
        this.description = description;
    }
}

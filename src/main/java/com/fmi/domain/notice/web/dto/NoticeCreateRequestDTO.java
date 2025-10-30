package com.fmi.domain.notice.web.dto;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import lombok.Data;

@Data
public class NoticeCreateRequestDTO {
    private String title;
    private String content;
    private NoticeCategory category;
    private Boolean pinned;
}



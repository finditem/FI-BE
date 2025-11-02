package com.fmi.domain.notice.web.dto;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeCreateRequestDTO {
    @NotBlank
    private String title;
    @NotBlank
    private String content;
    private NoticeCategory category;
    private Boolean pinned;
}



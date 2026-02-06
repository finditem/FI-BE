package com.fmi.domain.notice.web.dto;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NoticeUpdateRequestDTO {
    @Schema(description = "제목", example = "공지 수정 제목")
    @NotBlank
    private String title;
    @Schema(description = "내용", example = "공지 수정 내용입니다.")
    @NotBlank
    private String content;
    @Schema(description = "카테고리", example = "GENERAL")
    private NoticeCategory category;
    @Schema(description = "상단 고정 여부", example = "false")
    private Boolean pinned;
    @Schema(description = "이미지 URL 목록 (최대 5개)")
    @Size(max = 5)
    private List<String> imageUrls;
}

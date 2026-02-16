package com.fmi.domain.notice.web.dto;

import com.fmi.domain.notice.data.enums.NoticeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NoticeUpdateRequestDTO {
    @Schema(description = "제목 (미전송 시 기존 유지)", example = "공지 수정 제목")
    private String title;
    @Schema(description = "내용 (미전송 시 기존 유지)", example = "공지 수정 내용입니다.")
    private String content;
    @Schema(description = "카테고리", example = "GENERAL")
    private NoticeCategory category;
    @Schema(description = "상단 고정 여부", example = "false")
    private Boolean pinned;
    @Schema(description = "이미지 URL 목록 (최대 5개)")
    @Size(max = 5)
    private List<@NotBlank String> imageUrls;

    @Schema(description = "임시저장 여부 (true→임시저장, false→발행)", example = "false")
    private Boolean draft;
}

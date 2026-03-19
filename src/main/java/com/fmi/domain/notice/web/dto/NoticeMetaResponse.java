package com.fmi.domain.notice.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 메타데이터 응답")
public record NoticeMetaResponse(
        @Schema(description = "공지사항 제목")
        String title,
        @Schema(description = "공지사항 본문 요약 (최대 100자)")
        String description,
        @Schema(description = "공지사항 썸네일 이미지 URL (없으면 null)")
        String thumbnailUrl
) {
}

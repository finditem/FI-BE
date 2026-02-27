package com.fmi.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "관리자 신고/문의 통합 목록 응답")
public record AdminCsPageResponse(
        @Schema(description = "목록")
        List<AdminCsListResponse> items,
        @Schema(description = "다음 페이지 커서 (ISO datetime)")
        String nextCursor,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext
) {
}

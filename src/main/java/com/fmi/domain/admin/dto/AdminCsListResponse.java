package com.fmi.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 신고/문의 통합 목록 항목")
public record AdminCsListResponse(
        @Schema(description = "유형", example = "REPORT")
        AdminCsType type,
        @Schema(description = "ID")
        Long id,
        @Schema(description = "제목")
        String title,
        @Schema(description = "내용 미리보기 (100자)")
        String content,
        @Schema(description = "처리 상태", example = "PENDING")
        String status,
        @Schema(description = "작성자 닉네임")
        String writerNickname,
        @Schema(description = "작성자 이메일")
        String writerEmail,
        @Schema(description = "생성 시간")
        LocalDateTime createdAt
) {
}

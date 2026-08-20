package com.fmi.domain.user.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "활동 내역 항목")
public record ActivityResponse(
        @Schema(description = "활동 유형", example = "POST") String type,
        @Schema(description = "활동 ID") Long id,
        @Schema(description = "제목 또는 내용 요약") String title,
        @Schema(description = "내용 미리보기") String content,
        @Schema(description = "생성 시간") LocalDateTime createdAt) {}

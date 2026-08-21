package com.fmi.domain.map.web.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "최근 습득된 분실물 게시글")
public record RecentFoundPostResponse(
        @Schema(description = "게시글 ID", example = "12") Long postId,

        @Schema(description = "게시글 제목", example = "지갑을 습득했습니다")
        String title,

        @Schema(description = "썸네일 이미지 URL") String thumbnailImageUrl,

        @Schema(description = "게시글 생성 시간", example = "2026-02-20T14:32:10")
        LocalDateTime createdAt) {}

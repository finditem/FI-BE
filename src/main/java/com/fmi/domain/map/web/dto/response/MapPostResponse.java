package com.fmi.domain.map.web.dto.response;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "지도 게시글 카드 응답")
public record MapPostResponse(
        @Schema(description = "게시글 ID", example = "12") Long id,

        @Schema(description = "게시글 제목", example = "지갑을 습득했습니다")
        String title,

        @Schema(description = "게시글 요약", example = "검정색 지갑을 발견했습니다.")
        String summary,

        @Schema(description = "썸네일 이미지 URL", example = "https://image-url.com/image.jpg")
        String thumbnailImageUrl,

        @Schema(description = "주소") String address,

        @Schema(description = "게시글 상태", example = "SEARCHING")
        PostStatus postStatus,

        @Schema(description = "게시글 타입", example = "FOUND") PostType postType,

        @Schema(description = "카테고리", example = "WALLET") Category category,

        @Schema(description = "즐겨찾기 수", example = "5") Long favoriteCount,

        @Schema(description = "내가 즐겨찾기 했는지 여부", example = "true")
        boolean favoriteStatus,

        @Schema(description = "조회수", example = "120") Long viewCount,

        @Schema(description = "신규 게시글 여부", example = "true") boolean isNew,

        @Schema(description = "HOT 게시글 여부", example = "false")
        boolean isHot,

        @Schema(description = "생성 시간", example = "2026-02-20T14:32:10")
        LocalDateTime createdAt,

        @Schema(description = "이미지 개수", example = "3") Integer imageCount) {}

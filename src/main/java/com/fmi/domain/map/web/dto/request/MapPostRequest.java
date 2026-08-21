package com.fmi.domain.map.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지도 게시글 카드 조회 요청")
public record MapPostRequest(
        @Schema(description = "지도 줌 레벨 (1~11)", example = "5") @NotNull @Min(1) @Max(11) Integer level,

        @Schema(description = "게시글 타입 필터", example = "FOUND")
        PostType postType,

        @Schema(description = "게시글 상태 필터", example = "SEARCHING")
        PostStatus postStatus,

        @Schema(description = "카테고리 필터", example = "WALLET") Category category,

        @Schema(description = "검색 키워드", example = "지갑") String keyword) {}

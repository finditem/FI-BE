package com.fmi.domain.map.web.dto.request;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LocationMapPostRequest(
        @Schema(description = "지도 중심 위도", example = "37.5665")
        @NotNull
        Double latitude,

        @Schema(description = "지도 중심 경도", example = "126.9780")
        @NotNull
        Double longitude,

        @Schema(description = "지도 줌 레벨 (1~11)", example = "5")
        @NotNull
        @Min(1) @Max(11)
        Integer level,

        @Schema(description = "게시글 타입 필터", example = "FOUND")
        PostType postType,

        @Schema(description = "게시글 상태 필터", example = "SEARCHING")
        PostStatus postStatus,

        @Schema(description = "카테고리 필터", example = "WALLET")
        Category category
        ) {
}

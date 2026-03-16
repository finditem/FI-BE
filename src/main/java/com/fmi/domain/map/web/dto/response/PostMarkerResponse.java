package com.fmi.domain.map.web.dto.response;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "지도 마커 응답")
public record PostMarkerResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "게시글 위도", example = "37.5665")
        Double latitude,

        @Schema(description = "게시글 경도", example = "126.9780")
        Double longitude,

        @Schema(description = "카테고리", example = "WALLET")
        Category category,

        @Schema(description = "게시글 타입 (LOST / FOUND)", example = "FOUND")
        PostType postType,

        @Schema(description = "게시글 상태", example = "SEARCHING")
        PostStatus postStatus) {
}

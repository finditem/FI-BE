package com.fmi.domain.user.response;

import com.fmi.domain.post.web.dto.response.PostBriefResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내가 쓴 게시글 목록 응답")
public record MyPostPageResponse(
        @Schema(description = "게시글 목록")
        List<PostBriefResponse> posts,
        @Schema(description = "다음 페이지 커서")
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext
) {
}

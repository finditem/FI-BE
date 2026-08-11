package com.fmi.domain.user.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내가 쓴 댓글 목록 응답")
public record MyCommentPageResponse(
        @Schema(description = "댓글 목록")
        List<UserCommentSummaryResponse> comments,
        @Schema(description = "다음 페이지 커서")
        Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext
) {
}

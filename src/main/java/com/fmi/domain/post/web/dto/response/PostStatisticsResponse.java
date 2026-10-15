package com.fmi.domain.post.web.dto.response;

import com.fmi.domain.post.repository.PostStatisticsResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record PostStatisticsResponse(
        @Schema(description = "종료일 기준 전체 수", example = "0") long totalPostCount,

        @Schema(description = "기간 중 분실 게시글 최초 공개 수", example = "0")
        long lostPostCount,

        @Schema(description = "기간 중 발견 게시글 최초 공개 수", example = "0")
        long foundPostCount) {
    public static PostStatisticsResponse from(PostStatisticsResult result) {
        return new PostStatisticsResponse(result.totalPostCount(), result.lostPostCount(), result.foundPostCount());
    }
}
